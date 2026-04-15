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

        properties.put("pageNumber", Map.of(
                "type", "integer",
                "format", "int32",
                "description", "The number of page.",
                "default", 0
        ));

        properties.put("pageSize", Map.of(
                "type", "integer",
                "format", "int32",
                "description", "The size of page",
                "default", 100,
                "maximum", 1000
        ));

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

        paths.put(collectionPath, Map.of("post", createCreateOperation(tag, entity, scopePrefix)));
        paths.put(itemPath, Map.of(
                "get", createGetOperation(tag, entity, scopePrefix),
                "put", createUpdateOperation(tag, entity, scopePrefix),
                "delete", createDeleteOperation(tag, entity, scopePrefix)
        ));
        paths.put(searchPath, Map.of("post", createSearchOperation(tag, entity, scopePrefix)));
    }

    @SuppressWarnings("unchecked")
    private void createExternalPaths(Map<String, Object> spec,
                                     String resourcePath,
                                     String tag,
                                     String entity,
                                     String scopePrefix) {

        Map<String, Object> paths =
                (Map<String, Object>) spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());

        paths.put("/v1/" + resourcePath + "/{id}", Map.of(
                "get", createGetOperation(tag, entity, scopePrefix)
        ));
        paths.put("/v1/" + resourcePath + "/search", Map.of(
                "post", createSearchOperation(tag, entity, scopePrefix)
        ));
    }

    private Map<String, Object> createGetOperation(String tag, String entity, String scopePrefix) {
        return Map.of(
                "tags", List.of(tag),
                "operationId", "get" + entity + "ById",
                "summary", "Get " + entity + " by ID",
                "security", List.of(Map.of("oauth2", List.of(scopePrefix + ":read"))),
                "parameters", List.of(Map.of(
                        "in", "path",
                        "name", "id",
                        "required", true,
                        "schema", Map.of("type", "string")
                )),
                "responses", Map.of(
                        "200", successObjectResponse(entity + " found", entity),
                        "400", problemResponse("Invalid request"),
                        "404", problemResponse(entity + " not found")
                )
        );
    }

    private Map<String, Object> createCreateOperation(String tag, String entity, String scopePrefix) {
        return Map.of(
                "tags", List.of(tag),
                "operationId", "create" + entity,
                "summary", "Create " + entity,
                "security", List.of(Map.of("oauth2", List.of(scopePrefix + ":write"))),
                "requestBody", Map.of(
                        "required", true,
                        "content", Map.of(
                                "application/json",
                                Map.of("schema", Map.of("$ref", "#/components/schemas/" + entity))
                        )
                ),
                "responses", Map.of(
                        "201", successObjectResponse(entity + " created", entity),
                        "400", problemResponse("Validation failed")
                )
        );
    }

    private Map<String, Object> createUpdateOperation(String tag, String entity, String scopePrefix) {
        return Map.of(
                "tags", List.of(tag),
                "operationId", "update" + entity,
                "summary", "Update " + entity,
                "security", List.of(Map.of("oauth2", List.of(scopePrefix + ":write"))),
                "parameters", List.of(Map.of(
                        "in", "path",
                        "name", "id",
                        "required", true,
                        "schema", Map.of("type", "string")
                )),
                "requestBody", Map.of(
                        "required", true,
                        "content", Map.of(
                                "application/json",
                                Map.of("schema", Map.of("$ref", "#/components/schemas/" + entity))
                        )
                ),
                "responses", Map.of(
                        "200", successObjectResponse(entity + " updated", entity),
                        "400", problemResponse("Validation failed"),
                        "404", problemResponse(entity + " not found")
                )
        );
    }

    private Map<String, Object> createDeleteOperation(String tag, String entity, String scopePrefix) {
        return Map.of(
                "tags", List.of(tag),
                "operationId", "delete" + entity,
                "summary", "Delete " + entity,
                "security", List.of(Map.of("oauth2", List.of(scopePrefix + ":delete"))),
                "parameters", List.of(Map.of(
                        "in", "path",
                        "name", "id",
                        "required", true,
                        "schema", Map.of("type", "string")
                )),
                "responses", Map.of(
                        "204", Map.of("description", entity + " deleted"),
                        "400", problemResponse("Invalid request"),
                        "404", problemResponse(entity + " not found")
                )
        );
    }

    private Map<String, Object> createSearchOperation(String tag, String entity, String scopePrefix) {
        return Map.of(
                "tags", List.of(tag),
                "operationId", "search" + naming.upperFirst(naming.pluralPath(entity).replace("-", "")),
                "summary", "Search " + naming.pluralPath(entity),
                "security", List.of(Map.of("oauth2", List.of(scopePrefix + ":read"))),
                "requestBody", Map.of(
                        "required", false,
                        "content", Map.of(
                                "application/json",
                                Map.of("schema", Map.of(
                                        "$ref", "#/components/schemas/" + entity + "SearchCriteria"
                                ))
                        )
                ),
                "responses", Map.of(
                        "200", successArrayResponse("Search result for " + naming.pluralPath(entity), entity),
                        "400", problemResponse("Validation failed")
                )
        );
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
                props.put(relation.field(), Map.of(
                        "$ref", "#/components/schemas/" + relation.target()
                ));
            }
        }

        return schema;
    }

    private Map<String, Object> createEmptySchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>());
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
            servers.add(Map.of("url", "/api"));
            return servers;
        });

        Map<String, Object> components =
                (Map<String, Object>) spec.computeIfAbsent("components", k -> new LinkedHashMap<>());
        Map<String, Object> securitySchemes =
                (Map<String, Object>) components.computeIfAbsent("securitySchemes", k -> new LinkedHashMap<>());

        if (!securitySchemes.containsKey("oauth2")) {
            securitySchemes.put("oauth2", Map.of(
                    "type", "oauth2",
                    "flows", Map.of(
                            "clientCredentials", Map.of(
                                    "tokenUrl", "https://oauth.simple.api/token",
                                    "scopes", Map.of(
                                            scopePrefix + ":all", "Grants access to all operations",
                                            scopePrefix + ":read", "Grants read access",
                                            scopePrefix + ":write", "Grants write access",
                                            scopePrefix + ":delete", "Grants access to delete operations"
                                    )
                            )
                    )
            ));
        }

        Map<String, Object> schemas =
                (Map<String, Object>) components.computeIfAbsent("schemas", k -> new LinkedHashMap<>());

        schemas.putIfAbsent("ProblemDetailParam", Map.of(
                "type", "object",
                "properties", Map.of(
                        "key", Map.of("type", "string"),
                        "value", Map.of("type", "string")
                )
        ));

        schemas.putIfAbsent("ProblemDetailInvalidParam", Map.of(
                "type", "object",
                "properties", Map.of(
                        "name", Map.of("type", "string"),
                        "message", Map.of("type", "string")
                )
        ));

        schemas.putIfAbsent("ProblemDetailResponse", Map.of(
                "type", "object",
                "properties", Map.of(
                        "errorCode", Map.of("type", "string"),
                        "detail", Map.of("type", "string"),
                        "params", Map.of(
                                "type", "array",
                                "items", Map.of("$ref", "#/components/schemas/ProblemDetailParam")
                        ),
                        "invalidParams", Map.of(
                                "type", "array",
                                "items", Map.of("$ref", "#/components/schemas/ProblemDetailInvalidParam")
                        )
                )
        ));

        spec.computeIfAbsent("paths", k -> new LinkedHashMap<>());
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