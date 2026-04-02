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

    private static final String PROBLEM_REF = "#/components/schemas/ProblemDetailResponse";

    private final NamingService naming;

    public OpenApiService(NamingService naming) {
        this.naming = naming;
    }

    @SuppressWarnings("unchecked")
    public void addOrUpdateEntity(Path internalOpenApiFile,
                                  Path externalOpenApiFile,
                                  String scopePrefix,
                                  String entity,
                                  List<FieldDef> fields,
                                  List<RelationDef> relations,
                                  ApiDef apiDef) {

        Path internalFile = internalOpenApiFile.toAbsolutePath().normalize();
        Path externalFile = externalOpenApiFile.toAbsolutePath().normalize();

        Map<String, Object> internalSpec = loadYaml(internalFile);
        Map<String, Object> externalSpec = loadYaml(externalFile);

        ensureBase(internalSpec, scopePrefix, internalFile.getFileName().toString().replace(".yaml", ""));
        ensureBase(externalSpec, scopePrefix, externalFile.getFileName().toString().replace(".yaml", ""));

        String resourcePath = apiDef.path() != null ? apiDef.path() : naming.pluralPath(entity);

        String baseTag = apiDef.tag() != null
                ? apiDef.tag()
                : naming.lowerCamel(resourcePath.replace("-", ""));
        String internalTag = baseTag.endsWith("Internal") ? baseTag : baseTag + "Internal";
        String externalTag = baseTag.endsWith("Internal")
                ? baseTag.substring(0, baseTag.length() - "Internal".length())
                : baseTag;

        upsertEntitySchema(internalSpec, entity, fields, relations);
        upsertEntitySchema(externalSpec, entity, fields, relations);

        upsertSearchCriteriaSchema(internalSpec, entity, fields);
        upsertSearchCriteriaSchema(externalSpec, entity, fields);

        if (apiDef.expose()) {
            createInternalPaths(internalSpec, resourcePath, internalTag, entity, scopePrefix);
            createExternalPaths(externalSpec, resourcePath, externalTag, entity, scopePrefix);
        } else if (apiDef.parent() != null && apiDef.field() != null) {
            patchParentSchema(internalSpec, apiDef.parent(), apiDef.field(), apiDef.parentFieldCollection(), entity);
            patchParentSchema(externalSpec, apiDef.parent(), apiDef.field(), apiDef.parentFieldCollection(), entity);
        }

        saveYaml(internalFile, internalSpec);
        saveYaml(externalFile, externalSpec);
    }

    @SuppressWarnings("unchecked")
    private void upsertEntitySchema(Map<String, Object> spec,
                                    String entity,
                                    List<FieldDef> fields,
                                    List<RelationDef> relations) {
        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> schemas =
                (Map<String, Object>) components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());

        schemas.put(entity, createSchema(fields, relations));
    }

    @SuppressWarnings("unchecked")
    private void upsertSearchCriteriaSchema(Map<String, Object> spec,
                                            String entity,
                                            List<FieldDef> fields) {
        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> schemas =
                (Map<String, Object>) components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());

        Map<String, Object> schema = createEmptySchema();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        for (FieldDef field : fields) {
            properties.put(field.name(), createSimpleProperty(field.type()));
        }

        schemas.put(entity + "SearchCriteria", schema);
    }

    @SuppressWarnings("unchecked")
    private void patchParentSchema(Map<String, Object> spec,
                                   String parent,
                                   String field,
                                   boolean collection,
                                   String childSchemaName) {
        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> schemas =
                (Map<String, Object>) components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());

        Map<String, Object> parentSchema =
                (Map<String, Object>) schemas.computeIfAbsent(parent, k -> createEmptySchema());
        Map<String, Object> parentProperties =
                (Map<String, Object>) parentSchema.computeIfAbsent("properties", k -> new LinkedHashMap<>());

        if (collection) {
            Map<String, Object> array = new LinkedHashMap<>();
            array.put("type", "array");
            array.put("items", Map.of("$ref", "#/components/schemas/" + childSchemaName));
            parentProperties.put(field, array);
        } else {
            parentProperties.put(field, Map.of("$ref", "#/components/schemas/" + childSchemaName));
        }
    }

    @SuppressWarnings("unchecked")
    private void createInternalPaths(Map<String, Object> spec,
                                     String resourcePath,
                                     String tag,
                                     String entity,
                                     String scopePrefix) {
        Map<String, Object> paths =
                (Map<String, Object>) spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());

        String collectionPath = "/internal/" + resourcePath;
        String itemPath = collectionPath + "/{id}";
        String searchPath = collectionPath + "/search";

        Map<String, Object> collectionOps = new LinkedHashMap<>();
        collectionOps.put("post", createCreateOperation(tag, entity, scopePrefix));
        paths.put(collectionPath, collectionOps);

        Map<String, Object> itemOps = new LinkedHashMap<>();
        itemOps.put("get", createGetOperation(tag, entity, scopePrefix));
        itemOps.put("put", createUpdateOperation(tag, entity, scopePrefix));
        itemOps.put("delete", createDeleteOperation(tag, entity, scopePrefix));
        paths.put(itemPath, itemOps);

        Map<String, Object> searchOps = new LinkedHashMap<>();
        searchOps.put("post", createSearchOperation(tag, entity, scopePrefix));
        paths.put(searchPath, searchOps);
    }

    @SuppressWarnings("unchecked")
    private void createExternalPaths(Map<String, Object> spec,
                                     String resourcePath,
                                     String tag,
                                     String entity,
                                     String scopePrefix) {
        Map<String, Object> paths =
                (Map<String, Object>) spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());

        String itemPath = "/v1/" + resourcePath + "/{id}";
        String searchPath = "/v1/" + resourcePath + "/search";

        Map<String, Object> itemOps = new LinkedHashMap<>();
        itemOps.put("get", createGetOperation(tag, entity, scopePrefix));
        paths.put(itemPath, itemOps);

        Map<String, Object> searchOps = new LinkedHashMap<>();
        searchOps.put("post", createSearchOperation(tag, entity, scopePrefix));
        paths.put(searchPath, searchOps);
    }

    private Map<String, Object> createGetOperation(String tag, String entity, String scopePrefix) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));
        op.put("operationId", "get" + entity + "ById");
        op.put("summary", "Get " + entity + " by ID");
        op.put("security", List.of(Map.of("oauth2", List.of(scopePrefix + ":read"))));

        op.put("parameters", List.of(Map.of(
                "in", "path",
                "name", "id",
                "required", true,
                "schema", Map.of("type", "string")
        )));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("200", successObjectResponse(entity + " found", entity));
        responses.put("400", problemResponse("Invalid request"));
        responses.put("404", problemResponse(entity + " not found"));
        responses.put("500", problemResponse("Internal server error"));

        op.put("responses", responses);
        return op;
    }

    private Map<String, Object> createCreateOperation(String tag, String entity, String scopePrefix) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));
        op.put("operationId", "create" + entity);
        op.put("summary", "Create " + entity);
        op.put("security", List.of(Map.of("oauth2", List.of(scopePrefix + ":write"))));

        op.put("requestBody", Map.of(
                "required", true,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of("$ref", "#/components/schemas/" + entity))
                )
        ));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("201", successObjectResponse(entity + " created", entity));
        responses.put("400", problemResponse("Validation failed"));
        responses.put("409", problemResponse("Constraint conflict"));
        responses.put("500", problemResponse("Internal server error"));

        op.put("responses", responses);
        return op;
    }

    private Map<String, Object> createUpdateOperation(String tag, String entity, String scopePrefix) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));
        op.put("operationId", "update" + entity);
        op.put("summary", "Update " + entity);
        op.put("security", List.of(Map.of("oauth2", List.of(scopePrefix + ":write"))));

        op.put("parameters", List.of(Map.of(
                "in", "path",
                "name", "id",
                "required", true,
                "schema", Map.of("type", "string")
        )));

        op.put("requestBody", Map.of(
                "required", true,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of("$ref", "#/components/schemas/" + entity))
                )
        ));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("200", successObjectResponse(entity + " updated", entity));
        responses.put("400", problemResponse("Validation failed"));
        responses.put("404", problemResponse(entity + " not found"));
        responses.put("409", problemResponse("Optimistic lock conflict"));
        responses.put("500", problemResponse("Internal server error"));

        op.put("responses", responses);
        return op;
    }

    private Map<String, Object> createDeleteOperation(String tag, String entity, String scopePrefix) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));
        op.put("operationId", "delete" + entity);
        op.put("summary", "Delete " + entity);
        op.put("security", List.of(Map.of("oauth2", List.of(scopePrefix + ":delete"))));

        op.put("parameters", List.of(Map.of(
                "in", "path",
                "name", "id",
                "required", true,
                "schema", Map.of("type", "string")
        )));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("204", Map.of("description", entity + " deleted"));
        responses.put("400", problemResponse("Invalid request"));
        responses.put("404", problemResponse(entity + " not found"));
        responses.put("409", problemResponse("Constraint conflict"));
        responses.put("500", problemResponse("Internal server error"));

        op.put("responses", responses);
        return op;
    }

    private Map<String, Object> createSearchOperation(String tag, String entity, String scopePrefix) {
        Map<String, Object> op = new LinkedHashMap<>();
        op.put("tags", List.of(tag));
        op.put("operationId", "search" + naming.upperFirst(naming.pluralPath(entity).replace("-", "")));
        op.put("summary", "Search " + naming.pluralPath(entity));
        op.put("security", List.of(Map.of("oauth2", List.of(scopePrefix + ":read"))));

        op.put("parameters", List.of(
                Map.of("$ref", "#/components/parameters/limit"),
                Map.of("$ref", "#/components/parameters/offset")
        ));

        op.put("requestBody", Map.of(
                "required", false,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of("$ref", "#/components/schemas/" + entity + "SearchCriteria"))
                )
        ));

        Map<String, Object> responses = new LinkedHashMap<>();
        responses.put("200", successArrayResponse("Search result for " + naming.pluralPath(entity), entity));
        responses.put("400", problemResponse("Validation failed"));
        responses.put("500", problemResponse("Internal server error"));

        op.put("responses", responses);
        return op;
    }

    private Map<String, Object> successObjectResponse(String description, String schemaName) {
        return Map.of(
                "description", description,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of("$ref", "#/components/schemas/" + schemaName))
                )
        );
    }

    private Map<String, Object> successArrayResponse(String description, String itemSchemaName) {
        return Map.of(
                "description", description,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of(
                                "type", "array",
                                "items", Map.of("$ref", "#/components/schemas/" + itemSchemaName)
                        ))
                )
        );
    }

    private Map<String, Object> problemResponse(String description) {
        return Map.of(
                "description", description,
                "content", Map.of(
                        "application/json",
                        Map.of("schema", Map.of("$ref", PROBLEM_REF))
                )
        );
    }

    private Map<String, Object> createSchema(List<FieldDef> fields, List<RelationDef> relations) {
        Map<String, Object> schema = createEmptySchema();

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get("properties");

        props.put("id", Map.of("type", "string"));

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
            case "UUID" -> Map.of("type", "string", "format", "uuid");
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

    @SuppressWarnings("unchecked")
    private void ensureBase(Map<String, Object> spec, String scopePrefix, String serviceName) {
        spec.putIfAbsent("openapi", "3.0.3");

        spec.computeIfAbsent("info", k -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("title", serviceName);
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

