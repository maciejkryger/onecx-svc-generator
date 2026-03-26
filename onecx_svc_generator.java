///usr/bin/env jbang "$0" "$@" ; exit $?
//NOINTEGRATIONS
//DEPS io.quarkus.platform:quarkus-bom:3.32.3@pom
//DEPS io.quarkus:quarkus-picocli
//DEPS io.quarkus:quarkus-qute
//DEPS org.yaml:snakeyaml:2.3
//JAVAC_OPTIONS -parameters


//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/pom.xml.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/application.properties.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Dockerfile.jvm.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Dockerfile.native.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Chart.yaml.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/values.yaml.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/openapi-skeleton.yaml.tpl

//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Entity.java.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/DAO.java.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Service.java.tpl
//FILES https://raw.githubusercontent.com/maciejkryger/onecx-svc-generator/main/Liquibase-changelog.xml.tpl

import io.quarkus.runtime.annotations.QuarkusMain;
import io.quarkus.runtime.QuarkusApplication;
import picocli.CommandLine;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;

class onecx_svc_generator {

    public static void main(String... args) {
        int exit = new picocli.CommandLine(new Root()).execute(args);
        System.exit(exit);
    }

    @CommandLine.Command(
        name = "onecx-svc-generator",
        mixinStandardHelpOptions = true,
        description = "OneCX Service Generator CLI (Quarkus + Helm + Liquibase + OpenAPI)",
        subcommands = {
            CreateSvc.class,
            AddEntity.class,
            BatchModel.class
        }
    )
    static class Root implements Runnable {
        @Override public void run() { CommandLine.usage(this, System.out); }
    }

    @CommandLine.Command(name = "create-svc", description = "Generate full OneCX compliant Quarkus service structure")
    static class CreateSvc implements Runnable {
        @CommandLine.Option(names="--name", required=true) String name;
        @CommandLine.Option(names="--group", defaultValue="org.tkit.onecx") String group;
        @CommandLine.Option(names="--package", required=true) String pkg;
        @CommandLine.Option(names="--parent-version", defaultValue="0.72.0") String parentVersion;

        @Override public void run() {
            try {
                Path root = Path.of(name);
                Files.createDirectories(root);

                copyTemplate("pom.xml.tpl", root.resolve("pom.xml"), Map.of(
                        "name", name, "group", group, "package", pkg, "parentVersion", parentVersion));

                Files.createDirectories(root.resolve("src/main/resources"));
                copyTemplate("application.properties.tpl",
                        root.resolve("src/main/resources/application.properties"), Map.of("name", name));

                Path docker = root.resolve("src/main/docker"); Files.createDirectories(docker);
                copyTemplate("Dockerfile.jvm.tpl", docker.resolve("Dockerfile.jvm"), Map.of());
                copyTemplate("Dockerfile.native.tpl", docker.resolve("Dockerfile.native"), Map.of());

                Path helm = root.resolve("src/main/helm"); Files.createDirectories(helm);
                copyTemplate("Chart.yaml.tpl", helm.resolve("Chart.yaml"), Map.of("name", name));
                copyTemplate("values.yaml.tpl", helm.resolve("values.yaml"), Map.of("name", name));

                Path openapi = root.resolve("src/main/openapi"); Files.createDirectories(openapi);
                copyTemplate("openapi-skeleton.yaml.tpl",
                        openapi.resolve(name + "-v1.yaml"), Map.of("package", pkg, "name", name));

                System.out.println("✔ OneCX Service generated: " + root.toAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "add-entity", description = "Generate entity + DAO + service + liquibase changelog")
    static class AddEntity implements Runnable {
        @CommandLine.Option(names="--project", required=true) Path project;
        @CommandLine.Option(names="--package", required=true) String pkg;
        @CommandLine.Option(names="--entity", required=true) String entity;
        @CommandLine.Option(names="--fields") List<String> fieldsRaw;
        @CommandLine.Option(names="--relations") List<String> relationsRaw;

        @Override public void run() {
            try {
                List<Map<String,String>> fields = parseFields(fieldsRaw);
                List<Map<String,String>> rels = parseRelations(relationsRaw, fields);

                String fieldsDecl = buildFieldsDecl(fields);
                String relationsDecl = buildRelationsDecl(rels);
                String gettersSetters = buildGettersSetters(fields, rels);

                String liqCols = buildLiquibaseColumns(fields, rels);

                Map<String,Object> ctx = new HashMap<>();
                ctx.put("package", pkg);
                ctx.put("entity", entity);
                ctx.put("fieldsDecl", fieldsDecl);
                ctx.put("relationsDecl", relationsDecl);
                ctx.put("gettersSetters", gettersSetters);
                ctx.put("liquibaseColumns", liqCols);

                Path base = project.resolve("src/main/java/" + pkg.replace('.', '/'));
                Path db = project.resolve("src/main/resources/db");
                Files.createDirectories(base.resolve("entity"));
                Files.createDirectories(base.resolve("dao"));
                Files.createDirectories(base.resolve("service"));
                Files.createDirectories(db);

                copyTemplate("Entity.java.tpl", base.resolve("entity/" + entity + ".java"), ctx);
                copyTemplate("DAO.java.tpl", base.resolve("dao/" + entity + "DAO.java"), ctx);
                copyTemplate("Service.java.tpl", base.resolve("service/" + entity + "Service.java"), ctx);
                copyTemplate("Liquibase-changelog.xml.tpl", db.resolve("changelog-" + entity.toLowerCase() + ".xml"), ctx);

                System.out.println("✔ Entity generated: " + entity);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    @CommandLine.Command(name = "batch-model", description = "Generate multiple entities from YAML definition")
    static class BatchModel implements Runnable {
        @CommandLine.Option(names="--file", required=true) Path yamlFile;
        @CommandLine.Option(names="--project", required=true) Path project;
        @CommandLine.Option(names="--package", required=true) String pkg;

        @Override public void run() {
            try {
                var yaml = new org.yaml.snakeyaml.Yaml();
                Map<String,Object> doc = yaml.load(Files.newInputStream(yamlFile));
                List<Map<String,Object>> ents = (List<Map<String,Object>>) doc.get("entities");
                for (var e : ents) {
                    List<String> fields = toList(e.get("fields"));
                    List<String> rels = toList(e.get("relations"));

                    List<String> args = new ArrayList<>();

                    args.add("--project");
                    args.add(project.toString());

                    args.add("--package");
                    args.add(pkg);

                    args.add("--entity");
                    args.add(e.get("name").toString());

                    if (fields != null && !fields.isEmpty()) {
                        args.add("--fields");
                        args.add(String.join(" ", fields));
                    }

                    if (rels != null && !rels.isEmpty()) {
                        args.add("--relations");
                        args.add(String.join(" ", rels));
                    }

                    new CommandLine(new AddEntity()).execute(args.toArray(new String[0]));

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static Path scriptDir() {
        String src = System.getProperty("jbang.source");
        return src != null ? Path.of(src).getParent() : Paths.get(".");
    }

    static void copyTemplate(String sourceRel, Path target, Map<String,?> ctx) throws IOException {
        Path template = scriptDir().resolve(sourceRel).normalize();
        String out = Files.readString(template);
        for (var e : ctx.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", Objects.toString(e.getValue(), ""));
        }
        Files.createDirectories(target.getParent());
        Files.writeString(target, out);
    }

    static List<String> toList(Object o) {
        if (o == null) return null;
        List<String> out = new ArrayList<>();
        for (Object x : (List<?>) o) out.add(Objects.toString(x));
        return out;
    }

    static List<Map<String,String>> parseFields(List<String> raw) {
        List<Map<String,String>> list = new ArrayList<>();
        if (raw == null) return list;
        for (String r : raw) {
            for (String t : r.split("[ ,]+")) {
                if (t.isBlank()) continue;
                String[] arr = t.split(":");
                if (arr.length >= 2) list.add(Map.of("name", arr[0], "type", arr[1]));
            }
        }
        return list;
    }

    static List<Map<String,String>> parseRelations(List<String> raw, List<Map<String,String>> fields) {
        List<Map<String,String>> list = new ArrayList<>();
        if (raw == null) return list;
        for (String r : raw) {
            for (String t : r.split("[ ,]+")) {
                if (t.isBlank()) continue;
                String[] arr = t.split(":");
                String target = arr.length > 2 ? arr[2] : fields.stream().filter(f->f.get("name").equals(arr[0])).map(f->f.get("type")).findFirst().orElse("Object");
                list.add(Map.of("field", arr[0], "type", arr[1], "target", target));
            }
        }
        return list;
    }

    static String up(String s) { return s.substring(0,1).toUpperCase() + s.substring(1); }

    static String buildFieldsDecl(List<Map<String,String>> fields) {
        StringBuilder b = new StringBuilder();
        for (var f : fields) b.append("    private ").append(f.get("type")).append(" ").append(f.get("name")).append(";\n");
        return b.toString();
    }

    static String buildRelationsDecl(List<Map<String,String>> rels) {
        StringBuilder b = new StringBuilder();
        for (var r : rels) {
            b.append("    @").append(r.get("type")).append("\n");
            b.append("    private ").append(r.get("target")).append(" ").append(r.get("field")).append(";\n");
        }
        return b.toString();
    }

    static String buildGettersSetters(List<Map<String,String>> fields, List<Map<String,String>> rels) {
        StringBuilder b = new StringBuilder();
        for (var f : fields) {
            String n = f.get("name"), t = f.get("type");
            b.append("    public ").append(t).append(" get").append(up(n)).append("() { return ").append(n).append("; }\n");
            b.append("    public void set").append(up(n)).append("(").append(t).append(" ").append(n).append(") { this.").append(n).append(" = ").append(n).append("; }\n");
        }
        for (var r : rels) {
            String n = r.get("field"), t = r.get("target");
            b.append("    public ").append(t).append(" get").append(up(n)).append("() { return ").append(n).append("; }\n");
            b.append("    public void set").append(up(n)).append("(").append(t).append(" ").append(n).append(") { this.").append(n).append(" = ").append(n).append("; }\n");
        }
        return b.toString();
    }

    static String buildLiquibaseColumns(List<Map<String,String>> fields, List<Map<String,String>> rels) {
        StringBuilder b = new StringBuilder();
        for (var f : fields) {
            b.append("        <column name=\"").append(f.get("name")).append("\" type=\"")
             .append(mapLiquibaseType(f.get("type"))).append("\"/>\n");
        }
        for (var r : rels) {
            b.append("        <column name=\"").append(r.get("field")).append("_id\" type=\"BIGINT\"/>\n");
        }
        return b.toString();
    }

    static String mapLiquibaseType(String javaType) {
        return switch (javaType) {
            case "String" -> "VARCHAR(255)";
            case "Long", "long" -> "BIGINT";
            case "Integer", "int" -> "INT";
            case "BigDecimal" -> "NUMERIC(19,2)";
            case "LocalDateTime" -> "TIMESTAMP";
            case "Boolean", "boolean" -> "BOOLEAN";
            default -> "VARCHAR(255)";
        };
    }
}
