# onecx-svc-generator

Stable, JAR-based OneCX SVC generator.

## What it does

- creates a OneCX-like Quarkus service layout,
- generates a permission-aware OpenAPI contract,
- generates controllers + mappers + domain layer,
- relies on Maven/OpenAPI generation for REST interfaces and DTOs.

See `README-generator.md` for the full local and release workflow.


## structure
onecx-svc-generator/
├─ generator/
│  ├─ pom.xml
│  ├─ src/main/java/io/github/maciejkryger/onecxsvcgen/
│  │  ├─ Main.java
│  │  ├─ commands/
│  │  │  ├─ CreateSvcCommand.java
│  │  │  ├─ AddEntityCommand.java
│  │  │  └─ BatchModelCommand.java
│  │  ├─ service/
│  │  │  ├─ TemplateService.java
│  │  │  └─ ModelParserService.java
│  │  └─ model/
│  │     ├─ EntityDef.java
│  │     └─ RelationDef.java
│  ├─ src/main/resources/templates/
│  │  ├─ svc-project/
│  │  └─ entity/
│  └─ src/test/java/...
├─ launcher/
│  └─ onecx_svc_generator.java
└─ jbang-catalog.json

## build
cd generator
./mvnw package -Dquarkus.package.type=uber-jar

## run locally
java -jar target/onecx-svc-generator-1.0.0-runner.jar create-svc \
--name onecx-demo-svc \
--group org.tkit.onecx \
--package org.tkit.onecx.demo \
--parent-version 2.4.0

## run with jbang
jbang onecx-svc-generator@maciejkryger create-svc \
--name onecx-demo-svc \
--group org.tkit.onecx \
--package org.tkit.onecx.demo \
--parent-version 2.4.0

## run jbang with local jar
jbang app install onecx-svc-generator@maciejkryger

onecx-svc-generator create-svc \
--name onecx-demo-svc \
--group org.tkit.onecx \
--package org.tkit.onecx.demo \
--parent-version 2.4.0
