package org.tkit.onecx.onecxsvcgen.commands;

import org.tkit.onecx.onecxsvcgen.model.EntityDef;
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

    @Inject ModelParserService models;
    @Inject TemplateService templates;
    @Inject NamingService naming;
    @Inject OpenApiService openApi;

    @Override
    public void run() {
        List<EntityDef> entities = models.parseEntitiesYaml(yamlFile);
        String artifactId = project.getFileName().toString();
        String scopePrefix = naming.scopePrefixFromArtifactId(artifactId);

        for (EntityDef entityDef : entities) {
            try {
                openApi.addOrUpdateEntity(project.resolve("src/main/openapi/" + artifactId + "-v1.yaml"), scopePrefix,
                        entityDef.name(), entityDef.fields(), entityDef.relations(), entityDef.api());

                Map<String, Object> ctx = new HashMap<>();
                String entity = entityDef.name();
                String entityField = naming.lowerCamel(entity);
                String resourcePath = entityDef.api().path() != null ? entityDef.api().path() : naming.pluralPath(entity);
                String tag = entityDef.api().tag() != null ? entityDef.api().tag() : resourcePath;
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
                ctx.put("fieldsDecl", models.buildFieldsDecl(entityDef.fields()));
                ctx.put("relationsDecl", models.buildRelationsDecl(entityDef.relations(), pkg));
                ctx.put("gettersSetters", models.buildGettersSetters(entityDef.fields(), entityDef.relations(), pkg));
                ctx.put("liquibaseColumns", models.buildLiquibaseColumns(entityDef.fields(), entityDef.relations()));

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
                if (entityDef.aggregateRoot()) {
                    templates.renderToFile("templates/entity/Controller.java.tpl", base.resolve("rs/external/v1/controllers/" + entity + "Controller.java"), ctx);
                }
            } catch (Exception e) {
                throw new RuntimeException("batch-model failed while generating entity: " + entityDef.name(), e);
            }
        }
        System.out.println("✔ Generated " + entities.size() + " entities from model: " + yamlFile);
    }
}
