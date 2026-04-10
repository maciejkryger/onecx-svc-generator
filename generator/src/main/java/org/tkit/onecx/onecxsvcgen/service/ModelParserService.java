package org.tkit.onecx.onecxsvcgen.service;

import org.tkit.onecx.onecxsvcgen.model.ApiDef;
import org.tkit.onecx.onecxsvcgen.model.EntityDef;
import org.tkit.onecx.onecxsvcgen.model.FieldDef;
import org.tkit.onecx.onecxsvcgen.model.RelationDef;
import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ApplicationScoped
public class ModelParserService {

    public List<FieldDef> parseFields(List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }

        List<FieldDef> result = new ArrayList<>();
        for (String item : raw) {
            for (String token : item.split("[ ,]+")) {
                if (token.isBlank()) {
                    continue;
                }
                String[] arr = token.split(":");
                if (arr.length >= 2) {
                    result.add(new FieldDef(arr[0], arr[1]));
                }
            }
        }
        return result;
    }

    public List<RelationDef> parseRelations(List<String> raw) {
        if (raw == null) {
            return Collections.emptyList();
        }

        List<RelationDef> result = new ArrayList<>();
        for (String item : raw) {
            for (String token : item.split("[ ,]+")) {
                if (token.isBlank()) {
                    continue;
                }
                String[] arr = token.split(":");
                if (arr.length >= 3) {
                    result.add(new RelationDef(arr[0], arr[1], arr[2]));
                }
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<EntityDef> parseEntitiesYaml(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> doc = yaml.load(in);

            List<Map<String, Object>> entities = (List<Map<String, Object>>) doc.get("entities");
            List<EntityDef> out = new ArrayList<>();

            if (entities != null) {
                for (Map<String, Object> e : entities) {
                    String name = Objects.toString(e.get("name"));
                    boolean aggregateRoot = Boolean.parseBoolean(
                            Objects.toString(e.getOrDefault("aggregateRoot", "true"))
                    );

                    Map<String, Object> api = (Map<String, Object>) e.get("api");
                    ApiDef apiDef = api == null
                            ? new ApiDef(aggregateRoot, null, null, false, null, null)
                            : new ApiDef(
                            Boolean.parseBoolean(Objects.toString(api.getOrDefault("expose", aggregateRoot))),
                            stringOrNull(api.get("parent")),
                            stringOrNull(api.get("field")),
                            Boolean.parseBoolean(Objects.toString(api.getOrDefault("parentFieldCollection", false))),
                            stringOrNull(api.get("path")),
                            stringOrNull(api.get("tag"))
                    );

                    List<String> fields = toList(e.get("fields"));
                    List<String> relations = toList(e.get("relations"));

                    out.add(new EntityDef(
                            name,
                            aggregateRoot,
                            apiDef,
                            parseFields(fields),
                            parseRelations(relations)
                    ));
                }
            }

            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML model: " + file, e);
        }
    }

    private String stringOrNull(Object o) {
        return o == null ? null : Objects.toString(o);
    }

    private List<String> toList(Object o) {
        if (o == null) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        for (Object x : (List<?>) o) {
            out.add(Objects.toString(x));
        }
        return out;
    }

    public String tableName(String entity) {
        return dbName(entity);
    }

    private String dbName(String value) {
        return value
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace("-", "_")
                .toLowerCase();
    }

    public String buildEntityImports(List<FieldDef> fields) {
        Set<String> imports = new LinkedHashSet<>();

        for (FieldDef f : fields) {
            switch (f.type()) {
                case "BigDecimal" -> imports.add("import java.math.BigDecimal;");
                case "LocalDate" -> imports.add("import java.time.LocalDate;");
                case "LocalDateTime" -> imports.add("import java.time.LocalDateTime;");
                case "UUID" -> imports.add("import java.util.UUID;");
                default -> {
                    // no import needed
                }
            }
        }

        if (imports.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String i : imports) {
            sb.append(i).append("\n");
        }
        return sb.toString();
    }

    public String buildFieldsDecl(List<FieldDef> fields) {
        StringBuilder sb = new StringBuilder();
        for (FieldDef f : fields) {
            sb.append("    @Column(name = \"")
                    .append(dbName(f.name()))
                    .append("\")\n");
            sb.append("    private ")
                    .append(mapDomainType(f.type()))
                    .append(" ")
                    .append(f.name())
                    .append(";\n");
        }
        return sb.toString();
    }

    public String buildRelationsDecl(List<RelationDef> relations, String basePackage) {
        StringBuilder sb = new StringBuilder();
        for (RelationDef r : relations) {
            sb.append("    @").append(r.relationType()).append("\n");
            String targetType = basePackage + ".domain.models." + r.target();

            if ("ManyToOne".equals(r.relationType()) || "OneToOne".equals(r.relationType())) {
                sb.append("    @JoinColumn(name = \"")
                        .append(dbName(r.field()))
                        .append("_id\")\n");
                sb.append("    private ")
                        .append(targetType)
                        .append(" ")
                        .append(r.field())
                        .append(";\n");
            } else if ("OneToMany".equals(r.relationType()) || "ManyToMany".equals(r.relationType())) {
                sb.append("    private java.util.List<")
                        .append(targetType)
                        .append("> ")
                        .append(r.field())
                        .append(";\n");
            } else {
                sb.append("    private ")
                        .append(targetType)
                        .append(" ")
                        .append(r.field())
                        .append(";\n");
            }
        }
        return sb.toString();
    }

    public String buildLiquibaseColumns(List<FieldDef> fields, List<RelationDef> relations) {
        StringBuilder sb = new StringBuilder();

        for (FieldDef f : fields) {
            sb.append("            <column name=\"")
                    .append(dbName(f.name()))
                    .append("\" type=\"")
                    .append(mapLiquibaseType(f.type()))
                    .append("\"/>\n");
        }

        for (RelationDef r : relations) {
            if ("ManyToOne".equals(r.relationType()) || "OneToOne".equals(r.relationType())) {
                sb.append("            <column name=\"")
                        .append(dbName(r.field()))
                        .append("_id\" type=\"VARCHAR(36)\"/>\n");
            }
        }

        return sb.toString();
    }

    public String buildLiquibaseChangeSet(String entity, List<FieldDef> fields, List<RelationDef> relations) {
        String tableName = tableName(entity);

        StringBuilder sb = new StringBuilder();
        sb.append("    <changeSet id=\"create-").append(tableName).append("\" author=\"onecx-svc-generator\">\n");
        sb.append("        <createTable tableName=\"").append(tableName).append("\">\n");

        sb.append("            <column name=\"GUID\" type=\"VARCHAR(36)\">\n");
        sb.append("                <constraints primaryKey=\"true\" primaryKeyName=\"pk_")
                .append(tableName)
                .append("\" nullable=\"false\"/>\n");
        sb.append("            </column>\n");

        // inherited fields from TraceableEntity / base model
        sb.append("            <column name=\"creationDate\" type=\"TIMESTAMP\"/>\n");
        sb.append("            <column name=\"creationUser\" type=\"VARCHAR(255)\"/>\n");
        sb.append("            <column name=\"modificationDate\" type=\"TIMESTAMP\"/>\n");
        sb.append("            <column name=\"modificationUser\" type=\"VARCHAR(255)\"/>\n");
        sb.append("            <column name=\"OPTLOCK\" type=\"BIGINT\"/>\n");

        // local inherited/business field used in generated entities
        sb.append("            <column name=\"tenant\" type=\"VARCHAR(255)\"/>\n");

        sb.append(buildLiquibaseColumns(fields, relations));
        sb.append("        </createTable>\n");
        sb.append("    </changeSet>\n");

        return sb.toString();
    }

    public String buildLiquibaseChangeSets(List<EntityDef> entities) {
        StringBuilder sb = new StringBuilder();
        for (EntityDef entity : entities) {
            sb.append(buildLiquibaseChangeSet(entity.name(), entity.fields(), entity.relations())).append("\n");
        }
        return sb.toString();
    }

    public String buildFindByCriteriaPredicates(List<FieldDef> fields) {
        StringBuilder sb = new StringBuilder();

        for (FieldDef f : fields) {
            String upper = upper(f.name());
            String getter = "criteria.get" + upper + "()";

            switch (f.type()) {
                case "String" -> {
                    sb.append("        if (criteria != null && ")
                            .append(getter)
                            .append(" != null && !")
                            .append(getter)
                            .append(".isBlank()) {\n");
                    sb.append("            predicates.add(cb.like(cb.lower(root.get(\"")
                            .append(f.name())
                            .append("\")), \"%\" + ")
                            .append(getter)
                            .append(".toLowerCase() + \"%\"));\n");
                    sb.append("        }\n");
                }
                case "Integer", "int", "Long", "long", "BigDecimal", "Boolean", "boolean", "LocalDate", "LocalDateTime", "UUID" -> {
                    sb.append("        if (criteria != null && ")
                            .append(getter)
                            .append(" != null) {\n");
                    sb.append("            predicates.add(cb.equal(root.get(\"")
                            .append(f.name())
                            .append("\"), ")
                            .append(getter)
                            .append("));\n");
                    sb.append("        }\n");
                }
                default -> {
                    sb.append("        if (criteria != null && ")
                            .append(getter)
                            .append(" != null) {\n");
                    sb.append("            predicates.add(cb.equal(root.get(\"")
                            .append(f.name())
                            .append("\"), ")
                            .append(getter)
                            .append("));\n");
                    sb.append("        }\n");
                }
            }
        }

        return sb.toString();
    }

    public String buildRelationMappingMethods(List<RelationDef> relations, String pkg) {
        StringBuilder sb = new StringBuilder();
        String generatedModelPackage = generatedInternalModelPackage(pkg);
        String modelPackage = modelPackage(pkg);

        Set<String> processed = new LinkedHashSet<>();

        for (RelationDef relation : relations) {
            String target = relation.target();
            if (!processed.add(target)) {
                continue;
            }

            if (!isResolvableSingleRelation(relation)) {
                continue;
            }

            sb.append("    @Mapping(target = \"id\", ignore = true)\n");
            sb.append("    @Mapping(target = \"tenant\", ignore = true)\n");
            sb.append("    @Mapping(target = \"creationDate\", ignore = true)\n");
            sb.append("    @Mapping(target = \"creationUser\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationDate\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationUser\", ignore = true)\n");
            sb.append("    @Mapping(target = \"controlTraceabilityManual\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationCount\", ignore = true)\n");
            sb.append("    @Mapping(target = \"persisted\", ignore = true)\n");
            sb.append("    ")
                    .append(modelPackage).append(".").append(target)
                    .append(" fromDto(")
                    .append(generatedModelPackage).append(".").append(target).append("DTO dto);\n\n");

            sb.append("    @Mapping(target = \"id\", ignore = true)\n");
            sb.append("    @Mapping(target = \"tenant\", ignore = true)\n");
            sb.append("    @Mapping(target = \"creationDate\", ignore = true)\n");
            sb.append("    @Mapping(target = \"creationUser\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationDate\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationUser\", ignore = true)\n");
            sb.append("    @Mapping(target = \"controlTraceabilityManual\", ignore = true)\n");
            sb.append("    @Mapping(target = \"modificationCount\", ignore = true)\n");
            sb.append("    @Mapping(target = \"persisted\", ignore = true)\n");
            sb.append("    void update(")
                    .append(generatedModelPackage).append(".").append(target).append("DTO dto, ")
                    .append("@org.mapstruct.MappingTarget ")
                    .append(modelPackage).append(".").append(target)
                    .append(" entity);\n\n");
        }

        return sb.toString();
    }

    public String buildServiceRelationImports(List<RelationDef> relations, String pkg) {
        StringBuilder sb = new StringBuilder();
        Set<String> processed = new LinkedHashSet<>();

        for (RelationDef relation : relations) {
            if (!isResolvableSingleRelation(relation)) {
                continue;
            }

            String target = relation.target();
            if (!processed.add(target)) {
                continue;
            }

            sb.append("import ").append(daoPackage(pkg)).append(".").append(target).append("DAO;\n");
            sb.append("import ").append(modelPackage(pkg)).append(".").append(target).append(";\n");
        }

        return sb.toString();
    }

    public String buildRelationDaoInjections(List<RelationDef> relations) {
        StringBuilder sb = new StringBuilder();
        Set<String> processed = new LinkedHashSet<>();

        for (RelationDef relation : relations) {
            if (!isResolvableSingleRelation(relation)) {
                continue;
            }

            String target = relation.target();
            if (!processed.add(target)) {
                continue;
            }

            String var = lowerFirst(target) + "DAO";
            sb.append("    @Inject\n");
            sb.append("    ").append(target).append("DAO ").append(var).append(";\n\n");
        }

        return sb.toString();
    }

    public String buildRelationCreateResolvers(List<RelationDef> relations) {
        StringBuilder sb = new StringBuilder();

        for (RelationDef relation : relations) {
            if (!isResolvableSingleRelation(relation)) {
                continue;
            }

            String field = relation.field();
            String upperField = upper(field);
            String target = relation.target();
            String daoVar = lowerFirst(target) + "DAO";
            String resolvedVar = "resolved" + target;

            sb.append("        if (dto.get").append(upperField).append("() != null) {\n");
            sb.append("            if (dto.get").append(upperField).append("().getId() != null && !dto.get")
                    .append(upperField).append("().getId().isBlank()) {\n");
            sb.append("                ").append(target).append(" ").append(resolvedVar).append(" = ")
                    .append(daoVar).append(".findById(dto.get").append(upperField).append("().getId());\n");
            sb.append("                entity.set").append(upperField).append("(").append(resolvedVar).append(");\n");
            sb.append("            } else {\n");
            sb.append("                ").append(target).append(" ").append(resolvedVar)
                    .append(" = mapper.fromDto(dto.get").append(upperField).append("());\n");
            sb.append("                ").append(daoVar).append(".create(").append(resolvedVar).append(");\n");
            sb.append("                entity.set").append(upperField).append("(").append(resolvedVar).append(");\n");
            sb.append("            }\n");
            sb.append("        }\n");
        }

        return sb.toString();
    }

    public String buildRelationUpdateResolvers(List<RelationDef> relations) {
        return buildRelationCreateResolvers(relations);
    }

    public String modelPackage(String pkg) {
        return pkg + ".domain.models";
    }

    public String daoPackage(String pkg) {
        return pkg + ".domain.daos";
    }

    public String domainServicePackage(String pkg) {
        return pkg + ".domain.services";
    }

    public String controllerPackage(String pkg) {
        return pkg + ".rs.internal.controllers";
    }

    public String mapperPackage(String pkg) {
        return pkg + ".rs.internal.mappers";
    }

    public String externalControllerPackage(String pkg) {
        return pkg + ".rs.external.v1.controllers";
    }

    public String externalMapperPackage(String pkg) {
        return pkg + ".rs.external.v1.mappers";
    }

    public String generatedInternalApiPackage(String pkg) {
        return "gen." + pkg + ".rs.internal";
    }

    public String generatedInternalModelPackage(String pkg) {
        return "gen." + pkg + ".rs.internal.model";
    }

    public String generatedApiPackage(String pkg) {
        return "gen." + pkg + ".rs.external.v1";
    }

    public String generatedModelPackage(String pkg) {
        return "gen." + pkg + ".rs.external.v1.model";
    }

    public String mapDomainType(String javaType) {
        return switch (javaType) {
            case "String",
                 "Long", "long",
                 "Integer", "int",
                 "BigDecimal",
                 "Boolean", "boolean",
                 "LocalDate",
                 "LocalDateTime",
                 "UUID" -> javaType;
            default -> javaType;
        };
    }

    private String upper(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private String lowerFirst(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    private boolean isResolvableSingleRelation(RelationDef relation) {
        return "ManyToOne".equals(relation.relationType()) || "OneToOne".equals(relation.relationType());
    }

    private String mapLiquibaseType(String javaType) {
        return switch (javaType) {
            case "String" -> "VARCHAR(255)";
            case "Long", "long" -> "BIGINT";
            case "Integer", "int" -> "INT";
            case "BigDecimal" -> "NUMERIC(19,2)";
            case "LocalDateTime" -> "TIMESTAMP";
            case "LocalDate" -> "DATE";
            case "Boolean", "boolean" -> "BOOLEAN";
            case "UUID" -> "VARCHAR(36)";
            default -> "VARCHAR(255)";
        };
    }
}