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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class ModelParserService {

    public List<FieldDef> parseFields(List<String> raw) {
        if (raw == null) return Collections.emptyList();
        List<FieldDef> result = new ArrayList<>();
        for (String item : raw) {
            for (String token : item.split("[ ,]+")) {
                if (token.isBlank()) continue;
                String[] arr = token.split(":");
                if (arr.length >= 2) result.add(new FieldDef(arr[0], arr[1]));
            }
        }
        return result;
    }

    public List<RelationDef> parseRelations(List<String> raw) {
        if (raw == null) return Collections.emptyList();
        List<RelationDef> result = new ArrayList<>();
        for (String item : raw) {
            for (String token : item.split("[ ,]+")) {
                if (token.isBlank()) continue;
                String[] arr = token.split(":");
                if (arr.length >= 3) result.add(new RelationDef(arr[0], arr[1], arr[2]));
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
                    boolean aggregateRoot = Boolean.parseBoolean(Objects.toString(e.getOrDefault("aggregateRoot", "true")));
                    Map<String, Object> api = (Map<String, Object>) e.get("api");
                    ApiDef apiDef = api == null ? new ApiDef(aggregateRoot, null, null, false, null, null)
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
                    out.add(new EntityDef(name, aggregateRoot, apiDef, parseFields(fields), parseRelations(relations)));
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
        if (o == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (Object x : (List<?>) o) out.add(Objects.toString(x));
        return out;
    }

    public String buildFieldsDecl(List<FieldDef> fields) {
        StringBuilder sb = new StringBuilder();
        for (FieldDef f : fields) {
            sb.append("    private ").append(mapDomainType(f.type())).append(" ").append(f.name()).append(";\n");
        }
        return sb.toString();
    }

    public String buildRelationsDecl(List<RelationDef> relations, String basePackage) {
        StringBuilder sb = new StringBuilder();
        for (RelationDef r : relations) {
            sb.append("    @").append(r.relationType()).append("\n");
            String targetType = basePackage + ".domain.models." + r.target();
            if ("OneToMany".equals(r.relationType()) || "ManyToMany".equals(r.relationType())) {
                sb.append("    private java.util.List<").append(targetType).append("> ").append(r.field()).append(";\n");
            } else {
                sb.append("    private ").append(targetType).append(" ").append(r.field()).append(";\n");
            }
        }
        return sb.toString();
    }

    public String buildGettersSetters(List<FieldDef> fields, List<RelationDef> relations, String basePackage) {
        StringBuilder sb = new StringBuilder();
        for (FieldDef f : fields) {
            String upper = upper(f.name());
            String type = mapDomainType(f.type());
            sb.append("    public ").append(type).append(" get").append(upper).append("() { return ").append(f.name()).append("; }\n");
            sb.append("    public void set").append(upper).append("(").append(type).append(" ").append(f.name()).append(") { this.").append(f.name()).append(" = ").append(f.name()).append("; }\n");
        }
        for (RelationDef r : relations) {
            String upper = upper(r.field());
            String type = basePackage + ".domain.models." + r.target();
            String declared = ("OneToMany".equals(r.relationType()) || "ManyToMany".equals(r.relationType())) ? "java.util.List<" + type + ">" : type;
            sb.append("    public ").append(declared).append(" get").append(upper).append("() { return ").append(r.field()).append("; }\n");
            sb.append("    public void set").append(upper).append("(").append(declared).append(" ").append(r.field()).append(") { this.").append(r.field()).append(" = ").append(r.field()).append("; }\n");
        }
        return sb.toString();
    }

    public String buildLiquibaseColumns(List<FieldDef> fields, List<RelationDef> relations) {
        StringBuilder sb = new StringBuilder();
        for (FieldDef f : fields) {
            sb.append("            <column name=\"").append(f.name()).append("\" type=\"")
                    .append(mapLiquibaseType(f.type())).append("\"/>\n");
        }
        for (RelationDef r : relations) {
            if ("ManyToOne".equals(r.relationType()) || "OneToOne".equals(r.relationType())) {
                sb.append("            <column name=\"").append(r.field()).append("_id\" type=\"BIGINT\"/>\n");
            }
        }
        return sb.toString();
    }

    public String modelPackage(String pkg) { return pkg + ".domain.models"; }
    public String daoPackage(String pkg) { return pkg + ".domain.daos"; }
    public String domainServicePackage(String pkg) { return pkg + ".domain.services"; }
    public String controllerPackage(String pkg) { return pkg + ".rs.external.v1.controllers"; }
    public String mapperPackage(String pkg) { return pkg + ".rs.external.v1.mappers"; }
    public String generatedApiPackage(String pkg) { return "gen." + pkg + ".rs.external.v1"; }
    public String generatedModelPackage(String pkg) { return "gen." + pkg + ".rs.external.v1.model"; }

    public String mapDomainType(String javaType) {
        return switch (javaType) {
            case "String", "Long", "long", "Integer", "int", "BigDecimal", "Boolean", "boolean", "LocalDate", "LocalDateTime", "UUID" -> javaType;
            default -> javaType;
        };
    }

    private String upper(String s) { return s.substring(0, 1).toUpperCase() + s.substring(1); }

    private String mapLiquibaseType(String javaType) {
        return switch (javaType) {
            case "String" -> "VARCHAR(255)";
            case "Long", "long" -> "BIGINT";
            case "Integer", "int" -> "INT";
            case "BigDecimal" -> "NUMERIC(19,2)";
            case "LocalDateTime" -> "TIMESTAMP";
            case "LocalDate" -> "DATE";
            case "Boolean", "boolean" -> "BOOLEAN";
            default -> "VARCHAR(255)";
        };
    }
}
