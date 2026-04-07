package org.tkit.onecx.onecxsvcgen.commands;

import org.tkit.onecx.onecxsvcgen.service.BuildService;
import org.tkit.onecx.onecxsvcgen.service.GitHubReleaseService;
import org.tkit.onecx.onecxsvcgen.service.NamingService;
import org.tkit.onecx.onecxsvcgen.service.TemplateService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Command(name = "create-svc", description = "Generate a full OneCX-compliant Quarkus backend service")
public class CreateSvcCommand implements Runnable {

    @Option(names = "--name", required = true, description = "Artifact/repository name, e.g. onecx-demo-svc")
    String name;

    @Option(names = "--group", defaultValue = "org.tkit.onecx", description = "Maven groupId")
    String group;

    @Option(names = "--package", required = true, description = "Base Java package")
    String pkg;

    @Option(names = "--parent-version", description = "onecx-quarkus3-parent version; if omitted latest release is resolved automatically")
    String parentVersion;

    @Option(names = "--output-dir", description = "Directory where the service project should be generated")
    Path outputDir;

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
    GitHubReleaseService releases;

    @Inject
    NamingService naming;

    @Inject
    BuildService buildService;

    @Override
    public void run() {
        try {
            if (parentVersion == null || parentVersion.isBlank()) {
                parentVersion = releases.latestReleaseTag("onecx", "onecx-quarkus3-parent", "2.4.0");
            }

            Path baseDir = (outputDir != null ? outputDir : Path.of(".")).toAbsolutePath().normalize();
            Path root = baseDir.resolve(name).toAbsolutePath().normalize();
            Files.createDirectories(root);

            String scopePrefix = naming.scopePrefixFromArtifactId(name);

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("name", name);
            ctx.put("group", group);
            ctx.put("package", pkg);
            ctx.put("parentVersion", parentVersion);
            ctx.put("scopePrefix", scopePrefix);

            ctx.put("generatedApiPackage", "gen." + pkg + ".rs.external.v1");
            ctx.put("generatedModelPackage", "gen." + pkg + ".rs.external.v1.model");
            ctx.put("generatedInternalApiPackage", "gen." + pkg + ".rs.internal");
            ctx.put("generatedInternalModelPackage", "gen." + pkg + ".rs.internal.model");

            templates.renderToFile("templates/svc-project/pom.xml.tpl", root.resolve("pom.xml"), ctx);
            templates.renderToFile("templates/svc-project/gitignore.tpl", root.resolve(".gitignore"), ctx);
            templates.renderToFile("templates/svc-project/application.properties.tpl", root.resolve("src/main/resources/application.properties"), ctx);
            templates.renderToFile("templates/svc-project/Dockerfile.jvm.tpl", root.resolve("src/main/docker/Dockerfile.jvm"), ctx);
            templates.renderToFile("templates/svc-project/Dockerfile.native.tpl", root.resolve("src/main/docker/Dockerfile.native"), ctx);
            templates.renderToFile("templates/svc-project/Chart.yaml.tpl", root.resolve("src/main/helm/Chart.yaml"), ctx);
            templates.renderToFile("templates/svc-project/values.yaml.tpl", root.resolve("src/main/helm/values.yaml"), ctx);

            templates.renderToFile(
                    "templates/svc-project/openapi-skeleton.yaml.tpl",
                    root.resolve("src/main/openapi/" + name + "-internal.yaml"),
                    ctx
            );
            templates.renderToFile(
                    "templates/svc-project/openapi-skeleton.yaml.tpl",
                    root.resolve("src/main/openapi/" + name + "-external-v1.yaml"),
                    ctx
            );

            System.out.println("✔ Generated OneCX service: " + root);
            System.out.println("✔ Parent version: " + parentVersion);
            System.out.println("✔ Scope prefix: " + scopePrefix);

            if (build) {
                System.out.println("▶ Build requested, starting Maven build...");
                buildService.runMavenBuild(root);
            }
        } catch (Exception e) {
            throw new RuntimeException("create-svc failed", e);
        }
    }
}