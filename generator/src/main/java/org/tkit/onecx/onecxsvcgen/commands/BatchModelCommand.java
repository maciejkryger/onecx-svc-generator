package org.tkit.onecx.onecxsvcgen.commands;

import org.tkit.onecx.onecxsvcgen.model.EntityDef;
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

@Command(name = "batch-model", description = "Generate multiple entities from a YAML file")
public class BatchModelCommand implements Runnable {

    @Option(names = "--file", required = true, description = "YAML file describing entities")
    Path yamlFile;

    @Option(names = "--project", required = true, description = "Target generated service path")
    Path project;

    @Option(names = "--package", required = true, description = "Base Java package")
    String pkg;

    @Option(
            names = "--build",
            defaultValue = "false",
            arity = "1",
            description = "Run 'mvn clean package -DskipTests' in the generated project after generation"
    )
    boolean build;

    @Inject
    ModelParserService models;

    @Inject
    TemplateService templates;

    @Inject
    NamingService naming;

    @Inject
    OpenApiService openApi;

    @Inject
    BuildService buildService;

    @Override
    public void run() {
        Path projectPath = project.toAbsolutePath().normalize();
        Path modelFile = yamlFile.toAbsolutePath().normalize();

        List<EntityDef> entities = models.parseEntitiesYaml(modelFile);
        String artifactId = projectPath.getFileName().toString();
        String scopePrefix = naming.scopePrefixFromArtifactId(artifactId);

        Path internalSpec = projectPath.resolve("src/main/openapi/" + artifactId + "-internal.yaml");
        Path externalSpec = projectPath.resolve("src/main/openapi/" + artifactId + "-external-v1.yaml");

        for (EntityDef entityDef : entities) {
            try {
                openApi.addOrUpdateEntity(
                        internalSpec,
                        externalSpec,
                        scopePrefix,
                        entityDef.name(),
                        entityDef.fields(),
                        entityDef.relations(),
                        entityDef.api()
                );

                Map<String, Object> ctx = new HashMap<>();

                String entity = entityDef.name();
                String entityField = naming.lowerCamel(entity);
                String resourcePath = entityDef.api().path() != null
                        ? entityDef.api().path()
                        : naming.pluralPath(entity);
                String resourceOperationPlural = naming.upperFirst(resourcePath.replace("-", ""));

                String baseTag = entityDef.api().tag() != null
                        ? entityDef.api().tag()
                        : naming.lowerCamel(resourcePath.replace("-", ""));

                String internalTag = baseTag.endsWith("Internal")
                        ? baseTag
                        : baseTag + "Internal";

                String internalApiInterface = naming.apiInterfaceName(internalTag);
                String externalApiInterface = naming.apiInterfaceName(baseTag) + "V1Api";

                ctx.put("package", pkg);
                ctx.put("entity", entity);
                ctx.put("entityField", entityField);
                ctx.put("resourcePath", resourcePath);
                ctx.put("resourceOperationPlural", resourceOperationPlural);
                ctx.put("tableName", models.tableName(entity));
                ctx.put("entityImports", models.buildEntityImports(entityDef.fields()));

                // INTERNAL contract bindings
                ctx.put("resourceTag", internalTag);
                ctx.put("generatedApiPackage", models.generatedInternalApiPackage(pkg));
                ctx.put("generatedModelPackage", models.generatedInternalModelPackage(pkg));
                ctx.put("generatedApiInterface", internalApiInterface);
                ctx.put("generatedDto", entity + "DTO");

                // EXTERNAL placeholders for future split if needed
                ctx.put("generatedExternalApiPackage", models.generatedApiPackage(pkg));
                ctx.put("generatedExternalModelPackage", models.generatedModelPackage(pkg));
                ctx.put("generatedExternalDto", entity + "DTOV1");
                ctx.put("generatedExternalApiInterface", externalApiInterface);

                ctx.put("modelPackage", models.modelPackage(pkg));
                ctx.put("daoPackage", models.daoPackage(pkg));
                ctx.put("domainServicePackage", models.domainServicePackage(pkg));
                ctx.put("controllerPackage", models.controllerPackage(pkg));
                ctx.put("mapperPackage", models.mapperPackage(pkg));
                ctx.put("fieldsDecl", models.buildFieldsDecl(entityDef.fields()));
                ctx.put("relationsDecl", models.buildRelationsDecl(entityDef.relations(), pkg));
                ctx.put("liquibaseColumns", models.buildLiquibaseColumns(entityDef.fields(), entityDef.relations()));
                ctx.put("findByCriteriaPredicates", models.buildFindByCriteriaPredicates(entityDef.fields()));

                Path base = projectPath.resolve("src/main/java/" + pkg.replace('.', '/'));
                Files.createDirectories(base.resolve("domain/models"));
                Files.createDirectories(base.resolve("domain/daos"));
                Files.createDirectories(base.resolve("domain/services"));
                Files.createDirectories(base.resolve("rs/internal/controllers"));
                Files.createDirectories(base.resolve("rs/internal/mappers"));
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

                if (entityDef.api().expose()) {
                    templates.renderToFile(
                            "templates/entity/Controller.java.tpl",
                            base.resolve("rs/internal/controllers/" + entity + "Controller.java"),
                            ctx
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("batch-model failed while generating entity: " + entityDef.name(), e);
            }
        }

        System.out.println("✔ Generated " + entities.size() + " entities from model: " + modelFile);

        if (build) {
            buildService.runMavenBuild(projectPath);
        }
    }
}