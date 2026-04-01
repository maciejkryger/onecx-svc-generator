package org.tkit.onecx.onecxsvcgen.service;

import org.tkit.onecx.onecxsvcgen.model.ApiDef;
import org.tkit.onecx.onecxsvcgen.model.FieldDef;
import org.tkit.onecx.onecxsvcgen.model.RelationDef;
import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class OpenApiService {

    private final NamingService naming;

    public OpenApiService(NamingService naming) {
        this.naming = naming;
    }

    @SuppressWarnings("unchecked")
    public void addOrUpdateEntity(Path openApiFile,
                                  String scopePrefix,
                                  String entity,
                                  List<FieldDef> fields,
                                  List<RelationDef> relations,
                                  ApiDef apiDef) {
        Path normalizedFile = openApiFile.toAbsolutePath().normalize();

        Map<String, Object> spec = loadYaml(normalizedFile);
        ensureBase(spec, scopePrefix, normalizedFile.getFileName().toString().replace("-v1.yaml", ""));

        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> schemas =
                (Map<String, Object>) components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());
        Map<String, Object> paths =
                (Map<String, Object>) spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());

        String schemaName = entity;
        schemas.put(schemaName, createSchema(fields, relations));

        if (apiDef.expose()) {
            String resourcePath = apiDef.path() != null ? apiDef.path() : naming.pluralPath(entity);
            String tag = apiDef.tag() != null ? apiDef.tag() : resourcePath;
            createCrudPaths(paths, resourcePath, tag, schemaName, scopePrefix, entity);
        } else if (apiDef.parent() != null && apiDef.field() != null) {
            Map<String, Object> parentSchema =
                    (Map<String, Object>) schemas.computeIfAbsent(apiDef.parent(), k -> createEmptySchema());
            Map<String, Object> parentProperties =
                    (Map<String, Object>) parentSchema.computeIfAbsent("properties", k -> new LinkedHashMap<>());

            if (apiDef.parentFieldCollection()) {
                Map<String, Object> array = new LinkedHashMap<>();
                array.put("type", "array");

                Map<String, Object> items = new LinkedHashMap<>();
                items.put("$ref", "#/components/schemas/" + schemaName);

                array.put("items", items);
                parentProperties.put(apiDef.field(), array);
            } else {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("$ref", "#/components/schemas/" + schemaName);
                parentProperties.put(apiDef.field(), ref);
            }
        }

        saveYaml(normalizedFile, spec);
    }

    @SuppressWarnings("unchecked")
    public void ensureBase(Path file, String scopePrefix, String serviceName) {
        Path normalizedFile = file.toAbsolutePath().normalize();
        Map<String, Object> spec = normalizedFile.toFile().exists() ? loadYaml(normalizedFile) : new LinkedHashMap<>();
        ensureBase(spec, scopePrefix, serviceName);
        saveYaml(normalizedFile, spec);
    }

    @SuppressWarnings("unchecked")
    private void ensureBase(Map<String, Object> spec, String scopePrefix, String serviceName) {
        spec.putIfAbsent("openapi", "3.0.3");

        spec.computeIfAbsent("info", k -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("title", serviceName + " API");
            info.put("version", "1.0.0");
            return info;
        });

        spec.computeIfAbsent("servers", k -> {
            List<Map<String, Object>> servers = new ArrayList<>();
            Map<String, Object> server = new LinkedHashMap<>();
            server.put("url", "/api");
            servers.add(server);
            return servers;
        });

        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> securitySchemes =
                (Map<String, Object>) components.computeIfAbsent("securitySchemes", k -> new LinkedHashMap<>());

        if (!securitySchemes.containsKey("oauth2")) {
            Map<String, Object> oauth2 = new LinkedHashMap<>();
            oauth2.put("type", "oauth2");

            Map<String, Object> flows = new LinkedHashMap<>();
            Map<String, Object> clientCredentials = new LinkedHashMap<>();
            clientCredentials.put("tokenUrl", "https://oauth.simple.api/token");

            Map<String, Object> scopes = new LinkedHashMap<>();
            scopes.put(scopePrefix + ":all", "Grants access to all operations");
            scopes.put(scopePrefix + ":read", "Grants read access");
            scopes.put(scopePrefix + ":write", "Grants write access");
            scopes.put(scopePrefix + ":delete", "Grants access to delete operations");

            clientCredentials.put("scopes", scopes);
            flows.put("clientCredentials", clientCredentials);
            oauth2.put("flows", flows);

            securitySchemes.put("oauth2", oauth2);
        }

        spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());
        components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());
    }

    private Map<String, Object> createCrudOperation(String method,
                                                    String tag,
                                                    String schemaName,
                                                    String scopePrefix,
                                                    String entity,
                                                    boolean withId) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));

        op.put("operationId", switch (method) {
            case "get_all" -> "getAll" + naming.upperFirst(naming.pluralPath(entity).replace("-", ""));
            case "get_one" -> "get" + entity + "ById";
            case "post" -> "create" + entity;
            case "put" -> "update" + entity;
            case "delete" -> "delete" + entity;
            default -> entity;
        });

        op.put("summary", switch (method) {
            case "get_all" -> "Get all " + naming.pluralPath(entity);
            case "get_one" -> "Get " + entity + " by ID";
            case "post" -> "Create " + entity;
            case "put" -> "Update " + entity;
            case "delete" -> "Delete " + entity;
            default -> entity;
        });

        String scope = switch (method) {
            case "get_all", "get_one" -> scopePrefix + ":read";
            case "post", "put" -> scopePrefix + ":write";
            case "delete" -> scopePrefix + ":delete";
            default -> scopePrefix + ":all";
        };

        op.put("security", List.of(Map.of("oauth2", List.of(scope))));

        if (withId) {
            op.put("parameters", List.of(Map.of(
                    "in", "path",
                    "name", "id",
                    "required", true,
                    "schema", Map.of("type", "integer", "format", "int64")
            )));
        }

        if ("post".equals(method) || "put".equals(method)) {
            op.put("requestBody", Map.of(
                    "required", true,
                    "content", Map.of(
                            "application/json",
                            Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName))
                    )
            ));
        }

        Map<String, Object> responses = new LinkedHashMap<>();
        switch (method) {
            case "get_all" -> responses.put("200", Map.of(
                    "description", "List of " + naming.pluralPath(entity),
                    "content", Map.of(
                            "application/json",
                            Map.of("schema", Map.of(
                                    "type", "array",
                                    "items", Map.of("$ref", "#/components/schemas/" + schemaName)
                            ))
                    )
            ));
            case "get_one" -> {
                responses.put("200", Map.of(
                        "description", entity + " found",
                        "content", Map.of(
                                "application/json",
                                Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName))
                        )
                ));
                responses.put("404", Map.of("description", entity + " not found"));
            }
            case "post" -> responses.put("201", Map.of(
                    "description", entity + " created",
                    "content", Map.of(
                            "application/json",
                            Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName))
                    )
            ));
            case "put" -> responses.put("200", Map.of(
                    "description", entity + " updated",
                    "content", Map.of(
                            "application/json",
                            Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName))
                    )
            ));
            case "delete" -> responses.put("204", Map.of("description", entity + " deleted"));
        }

        op.put("responses", responses);
        return op;
    }

    private void createCrudPaths(Map<String, Object> paths,
                                 String resourcePath,
                                 String tag,
                                 String schemaName,
                                 String scopePrefix,
                                 String entity) {
        String listPath = "/v1/" + resourcePath;
        String itemPath = listPath + "/{id}";

        Map<String, Object> listOps = new LinkedHashMap<>();
        listOps.put("get", createCrudOperation("get_all", tag, schemaName, scopePrefix, entity, false));
        listOps.put("post", createCrudOperation("post", tag, schemaName, scopePrefix, entity, false));

        Map<String, Object> itemOps = new LinkedHashMap<>();
        itemOps.put("get", createCrudOperation("get_one", tag, schemaName, scopePrefix, entity, true));
        itemOps.put("put", createCrudOperation("put", tag, schemaName, scopePrefix, entity, true));
        itemOps.put("delete", createCrudOperation("delete", tag, schemaName, scopePrefix, entity, true));

        paths.put(listPath, listOps);
        paths.put(itemPath, itemOps);
    }

    private Map<String, Object> createSchema(List<FieldDef> fields, List<RelationDef> relations) {
        Map<String, Object> schema = createEmptySchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        props.put("id", Map.of("type", "integer", "format", "int64"));

        for (FieldDef field : fields) {
            props.put(field.name(), createSimpleProperty(field.type()));
        }

        for (RelationDef relation : relations) {
            if ("OneToMany".equals(relation.relationType()) || "ManyToMany".equals(relation.relationType())) {
                props.put(relation.field(), Map.of(
                        "type", "array",
                        "items", Map.of("$ref", "#/components/schemas/" + relation.target())
                ));
            } else {
                props.put(relation.field(), Map.of("$ref", "#/components/schemas/" + relation.target()));
            }
        }

        return schema;
    }

    private Map<String, Object> createEmptySchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<String, Object>());
        return schema;
    }

    private Map<String, Object> createSimpleProperty(String type) {
        return switch (type) {
            case "String" -> Map.of("type", "string");
            case "Integer", "int" -> Map.of("type", "integer", "format", "int32");
            case "Long", "long" -> Map.of("type", "integer", "format", "int64");
            case "BigDecimal" -> Map.of("type", "number", "format", "double");
            case "Boolean", "boolean" -> Map.of("type", "boolean");
            case "LocalDate" -> Map.of("type", "string", "format", "date");
            case "LocalDateTime" -> Map.of("type", "string", "format", "date-time");
            default -> Map.of("type", "string");
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(Path file) {
        try (InputStream in = Files.newInputStream(file.toAbsolutePath().normalize())) {
            Yaml yaml = new Yaml();
            Object obj = yaml.load(in);
            if (obj == null) {
                return new LinkedHashMap<>();
            }
            return (Map<String, Object>) obj;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load OpenAPI file: " + file, e);
        }
    }

    private void saveYaml(Path file, Map<String, Object> spec) {
        try {
            DumperOptions options = new DumperOptions();
            options.setPrettyFlow(true);
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setIndent(2);
            options.setIndicatorIndent(1);
            options.setWidth(160);

            Yaml yaml = new Yaml(options);

            Path normalized = file.toAbsolutePath().normalize();
            Files.createDirectories(normalized.getParent());
            Files.writeString(normalized, yaml.dump(spec));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save OpenAPI file: " + file, e);
        }
    }
}