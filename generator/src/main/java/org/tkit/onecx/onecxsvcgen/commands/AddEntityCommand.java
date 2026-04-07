package org.tkit.onecx.onecxsvcgen.commands;

import org.tkit.onecx.onecxsvcgen.model.ApiDef;
import org.tkit.onecx.onecxsvcgen.model.FieldDef;
import org.tkit.onecx.onecxsvcgen.model.RelationDef;
import org.tkit.onecx.onecxsvcgen.service.BuildService;
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

@Command(
        name = "add-entity",
        description = "Generate domain layer and update internal/external API contracts. " +
                "Root entities get CRUD in internal and read/search in external-v1; " +
                "child entities extend parent schemas."
)
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

    @Option(
            names = "--root",
            defaultValue = "true",
            arity = "1",
            description = "true = entity gets standalone API CRUD; false = child component extends parent schema only"
    )
    boolean root;

    @Option(names = "--api-parent", description = "Parent aggregate root name if --root=false")
    String apiParent;

    @Option(names = "--api-field", description = "Field name to add to the parent schema if --root=false")
    String apiField;

    @Option(
            names = "--api-parent-collection",
            defaultValue = "false",
            arity = "1",
            description = "true if parent field should be an array of the child DTO"
    )
    boolean apiParentCollection;

    @Option(names = "--api-path", description = "Override the resource path for root entities, e.g. chats")
    String apiPath;

    @Option(names = "--api-tag", description = "Override the external OpenAPI tag base for the entity")
    String apiTag;

    @Option(
            names = "--build",
            defaultValue = "false",
            fallbackValue = "true",
            arity = "0..1",
            description = "Run 'mvn clean package -DskipTests' in the generated project after generation"
    )
    boolean build;

    @Inject
    TemplateService templates;

    @Inject
    ModelParserService models;

    @Inject
    NamingService naming;

    @Inject
    OpenApiService openApi;

    @Inject
    BuildService buildService;

    @Override
    public void run() {
        try {
            Path projectPath = project.toAbsolutePath().normalize();

            List<FieldDef> fields = models.parseFields(fieldsRaw);
            List<RelationDef> relations = models.parseRelations(relationsRaw);

            String artifactId = projectPath.getFileName().toString();
            String scopePrefix = naming.scopePrefixFromArtifactId(artifactId);

            ApiDef api = new ApiDef(root, apiParent, apiField, apiParentCollection, apiPath, apiTag);

            Path internalSpec = projectPath.resolve("src/main/openapi/" + artifactId + "-internal.yaml");
            Path externalSpec = projectPath.resolve("src/main/openapi/" + artifactId + "-external-v1.yaml");

            openApi.addOrUpdateEntity(
                    internalSpec,
                    externalSpec,
                    scopePrefix,
                    entity,
                    fields,
                    relations,
                    api
            );

            Map<String, Object> ctx = new HashMap<>();

            String entityField = naming.lowerCamel(entity);
            String resourcePath = api.path() != null ? api.path() : naming.pluralPath(entity);
            String resourceOperationPlural = naming.upperFirst(resourcePath.replace("-", ""));

            String baseTag = api.tag() != null
                    ? api.tag()
                    : naming.lowerCamel(resourcePath.replace("-", ""));

            String internalTag = baseTag.endsWith("Internal")
                    ? baseTag
                    : baseTag + "Internal";

            String internalApiInterface = naming.apiInterfaceName(internalTag);
            String externalApiInterface = naming.upperFirst(baseTag) + "V1Api";

            ctx.put("package", pkg);
            ctx.put("entity", entity);
            ctx.put("entityField", entityField);
            ctx.put("resourcePath", resourcePath);
            ctx.put("resourceOperationPlural", resourceOperationPlural);
            ctx.put("tableName", models.tableName(entity));
            ctx.put("entityImports", models.buildEntityImports(fields));

            // INTERNAL contract bindings
            ctx.put("controllerPackage", models.controllerPackage(pkg));
            ctx.put("mapperPackage", models.mapperPackage(pkg));
            ctx.put("resourceTag", internalTag);
            ctx.put("generatedApiPackage", models.generatedInternalApiPackage(pkg));
            ctx.put("generatedModelPackage", models.generatedInternalModelPackage(pkg));
            ctx.put("generatedApiInterface", internalApiInterface);
            ctx.put("generatedDto", entity + "DTO");

            // EXTERNAL placeholders for future split if needed
            ctx.put("externalControllerPackage", models.externalControllerPackage(pkg));
            ctx.put("externalMapperPackage", models.externalMapperPackage(pkg));
            ctx.put("generatedExternalApiPackage", models.generatedApiPackage(pkg));
            ctx.put("generatedExternalModelPackage", models.generatedModelPackage(pkg));
            ctx.put("generatedExternalDto", entity + "DTOV1");
            ctx.put("generatedExternalApiInterface", externalApiInterface);

            ctx.put("modelPackage", models.modelPackage(pkg));
            ctx.put("daoPackage", models.daoPackage(pkg));
            ctx.put("domainServicePackage", models.domainServicePackage(pkg));
            ctx.put("fieldsDecl", models.buildFieldsDecl(fields));
            ctx.put("relationsDecl", models.buildRelationsDecl(relations, pkg));
            ctx.put("liquibaseColumns", models.buildLiquibaseColumns(fields, relations));
            ctx.put("findByCriteriaPredicates", models.buildFindByCriteriaPredicates(fields));
            ctx.put("generatedInternalSearchCriteria", entity + "SearchCriteriaDTO");
            ctx.put("relationMappingMethods", models.buildRelationMappingMethods(relations, pkg));

            Path base = projectPath.resolve("src/main/java/" + pkg.replace('.', '/'));
            Files.createDirectories(base.resolve("domain/models"));
            Files.createDirectories(base.resolve("domain/daos"));
            Files.createDirectories(base.resolve("domain/services"));
            Files.createDirectories(base.resolve("rs/internal/controllers"));
            Files.createDirectories(base.resolve("rs/internal/mappers"));
            Files.createDirectories(base.resolve("rs/external/v1/controllers"));
            Files.createDirectories(base.resolve("rs/external/v1/mappers"));
            Files.createDirectories(projectPath.resolve("src/main/resources/db"));

            templates.renderToFile(
                    "templates/entity/Entity.java.tpl",
                    base.resolve("domain/models/" + entity + ".java"),
                    ctx
            );
            templates.renderToFile(
                    "templates/entity/DAO.java.tpl",
                    base.resolve("domain/daos/" + entity + "DAO.java"),
                    ctx
            );
            templates.renderToFile(
                    "templates/entity/Service.java.tpl",
                    base.resolve("domain/services/" + entity + "Service.java"),
                    ctx
            );
            templates.renderToFile(
                    "templates/entity/Liquibase-changelog.xml.tpl",
                    projectPath.resolve("src/main/resources/db/changelog-" + entity.toLowerCase() + ".xml"),
                    ctx
            );
            templates.renderToFile(
                    "templates/entity/Mapper.java.tpl",
                    base.resolve("rs/internal/mappers/" + entity + "Mapper.java"),
                    ctx
            );
            templates.renderToFile(
                    "templates/entity/ExceptionMapper.java.tpl",
                    base.resolve("rs/internal/mappers/ExceptionMapper.java"),
                    ctx
            );

                        if (root) {
                templates.renderToFile(
                        "templates/entity/Controller.java.tpl",
                        base.resolve("rs/internal/controllers/" + entity + "Controller.java"),
                        ctx
                );
            }

            templates.renderToFile(
                    "templates/entity/ExternalMapper.java.tpl",
                    base.resolve("rs/external/v1/mappers/" + entity + "Mapper.java"),
                    ctx
            );

            templates.renderToFile(
                    "templates/entity/ExternalExceptionMapper.java.tpl",
                    base.resolve("rs/external/v1/mappers/ExceptionMapper.java"),
                    ctx
            );

            if (root) {
                templates.renderToFile(
                        "templates/entity/ExternalController.java.tpl",
                        base.resolve("rs/external/v1/controllers/" + entity + "Controller.java"),
                        ctx
                );
            }

            System.out.println("✔ Generated domain layer for: " + entity);
            if (root) {
                System.out.println("✔ Updated internal API (CRUD + search) and external-v1 API (get + search) for: " + entity);
            } else {
                System.out.println(
                        "✔ Added component schema " + entity + " to parent API " + apiParent
                                + " in internal and external-v1 contracts. No standalone CRUD paths created."
                );
            }

            if (build) {
                buildService.runMavenBuild(projectPath);
            }
        } catch (Exception e) {
            throw new RuntimeException("add-entity failed", e);
        }
    }
}