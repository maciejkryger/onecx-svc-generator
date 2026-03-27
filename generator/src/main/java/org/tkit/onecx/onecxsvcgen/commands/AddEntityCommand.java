package org.tkit.onecx.onecxsvcgen.commands;

import org.tkit.onecx.onecxsvcgen.model.ApiDef;
import org.tkit.onecx.onecxsvcgen.model.FieldDef;
import org.tkit.onecx.onecxsvcgen.model.RelationDef;
import org.tkit.onecx.onecxsvcgen.service.ModelParserService;
import org.tkit.onecx.onecxsvcgen.service.NamingService;
import org.tkit.onecx.onecxsvcgen.service.OpenApiService;
import org.tkit.onecx.onecxsvcgen.service.TemplateService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Command(name = "add-entity", description = "Generate domain layer and update API contract. Root entities get CRUD paths; child entities only extend parent schemas.")
public class AddEntityCommand implements Runnable {

    @Option(names = "--project", required = true, description = "Path to an existing generated service")
    Path project;

    @Option(names = "--package", required = true, description = "Base Java package")
    String pkg;

    @Option(names = "--entity", required = true, description = "Entity name")
    String entity;

    @Option(names = "--fields", split = ",", description = "Fields, e.g. name:String,price:BigDecimal")
    List<String> fieldsRaw;

    @Option(names = "--relations", split = ",", description = "Relations, e.g. category:ManyToOne:Category")
    List<String> relationsRaw;

    @Option(names = "--root", defaultValue = "true", description = "true = entity gets standalone API CRUD; false = child component extends parent schema only")
    boolean root;

    @Option(names = "--api-parent", description = "Parent aggregate root name if --root=false")
    String apiParent;

    @Option(names = "--api-field", description = "Field name to add to the parent schema if --root=false")
    String apiField;

    @Option(names = "--api-parent-collection", defaultValue = "false", description = "true if parent field should be an array of the child DTO")
    boolean apiParentCollection;

    @Option(names = "--api-path", description = "Override the resource path for root entities, e.g. chats")
    String apiPath;

    @Option(names = "--api-tag", description = "Override the OpenAPI tag for root entities")
    String apiTag;

    @Inject TemplateService templates;
    @Inject ModelParserService models;
    @Inject NamingService naming;
    @Inject OpenApiService openApi;

    @Override
    public void run() {
        try {
            List<FieldDef> fields = models.parseFields(fieldsRaw);
            List<RelationDef> relations = models.parseRelations(relationsRaw);

            String artifactId = project.getFileName().toString();
            String scopePrefix = naming.scopePrefixFromArtifactId(artifactId);

            ApiDef api = new ApiDef(root, apiParent, apiField, apiParentCollection, apiPath, apiTag);
            openApi.addOrUpdateEntity(project.resolve("src/main/openapi/" + artifactId + "-v1.yaml"), scopePrefix, entity, fields, relations, api);

            Map<String, Object> ctx = new HashMap<>();
            String entityField = naming.lowerCamel(entity);
            String resourcePath = api.path() != null ? api.path() : naming.pluralPath(entity);
            String tag = api.tag() != null ? api.tag() : resourcePath;
            String apiInterface = naming.apiInterfaceName(tag);
            String resourceOperationPlural = naming.upperFirst(resourcePath.replace("-", ""));

            ctx.put("package", pkg);
            ctx.put("entity", entity);
            ctx.put("entityField", entityField);
            ctx.put("resourceTag", tag);
            ctx.put("resourcePath", resourcePath);
            ctx.put("resourceOperationPlural", resourceOperationPlural);
            ctx.put("generatedApiPackage", models.generatedApiPackage(pkg));
            ctx.put("generatedModelPackage", models.generatedModelPackage(pkg));
            ctx.put("generatedApiInterface", apiInterface);
            ctx.put("generatedDto", entity + "DTO");
            ctx.put("modelPackage", models.modelPackage(pkg));
            ctx.put("daoPackage", models.daoPackage(pkg));
            ctx.put("domainServicePackage", models.domainServicePackage(pkg));
            ctx.put("controllerPackage", models.controllerPackage(pkg));
            ctx.put("mapperPackage", models.mapperPackage(pkg));
            ctx.put("fieldsDecl", models.buildFieldsDecl(fields));
            ctx.put("relationsDecl", models.buildRelationsDecl(relations, pkg));
            ctx.put("gettersSetters", models.buildGettersSetters(fields, relations, pkg));
            ctx.put("liquibaseColumns", models.buildLiquibaseColumns(fields, relations));

            Path base = project.resolve("src/main/java/" + pkg.replace('.', '/'));
            Files.createDirectories(base.resolve("domain/models"));
            Files.createDirectories(base.resolve("domain/daos"));
            Files.createDirectories(base.resolve("domain/services"));
            Files.createDirectories(base.resolve("rs/external/v1/controllers"));
            Files.createDirectories(base.resolve("rs/external/v1/mappers"));
            Files.createDirectories(project.resolve("src/main/resources/db"));

            templates.renderToFile("templates/entity/Entity.java.tpl", base.resolve("domain/models/" + entity + ".java"), ctx);
            templates.renderToFile("templates/entity/DAO.java.tpl", base.resolve("domain/daos/" + entity + "DAO.java"), ctx);
            templates.renderToFile("templates/entity/DomainService.java.tpl", base.resolve("domain/services/" + entity + "Service.java"), ctx);
            templates.renderToFile("templates/entity/Liquibase-changelog.xml.tpl", project.resolve("src/main/resources/db/changelog-" + entity.toLowerCase() + ".xml"), ctx);
            templates.renderToFile("templates/entity/Mapper.java.tpl", base.resolve("rs/external/v1/mappers/" + entity + "Mapper.java"), ctx);
            if (root) {
                templates.renderToFile("templates/entity/Controller.java.tpl", base.resolve("rs/external/v1/controllers/" + entity + "Controller.java"), ctx);
            }

            System.out.println("✔ Generated domain layer for: " + entity);
            if (root) {
                System.out.println("✔ Added standalone API CRUD for: " + entity);
            } else {
                System.out.println("✔ Added component schema " + entity + " to parent API " + apiParent + ". No standalone CRUD paths created.");
            }
        } catch (Exception e) {
            throw new RuntimeException("add-entity failed", e);
        }
    }
}
