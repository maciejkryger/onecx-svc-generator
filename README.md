# OneCX SVC Generator

Generator of OneCX-like Quarkus services, based on a custom template engine and OpenAPI generation.
Java version 21, Quarkus 3.2, OpenAPI Generator 7.0.1.

## What it does

- creates a OneCX-like Quarkus service layout,
- generates a permission-aware OpenAPI contract,
- generates controllers + mappers + domain layer,
- relies on Maven/OpenAPI generation for REST interfaces and DTOs.

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


## Local workflow

### 1. Build the generator
```bash
cd /home/Maciej/projects/onecx/onecx-svc-generator/generator
mvn clean package -Dquarkus.package.type=uber-jar
```

### 2. Generate a new service
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar create-svc   --name onecx-demo-svc   --group org.tkit.onecx   --package org.tkit.onecx.demo
```
#### with autobuild - recomended for development, as it compiles the generated code after each change:
```bash 
cd /home/Maciej/projects/onecx

java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar create-svc \
  --name onecx-demo-svc \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --build true
```

### 3. Add a root entity (creates API + controller + mapper + domain layer)
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar add-entity \
  --project /home/Maciej/projects/onecx/onecx-demo-svc \
  --package org.tkit.onecx.demo \
  --entity Product \
  --fields name:String,price:BigDecimal \
  --root true \
  --build true
```

### 4. Add a child entity/component (updates existing API schema, no standalone CRUD)
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar add-entity \
  --project /home/Maciej/projects/onecx/onecx-demo-svc \
  --package org.tkit.onecx.demo \
  --entity ProductItem \
  --fields quantity:Integer,position:Integer \
  --root false \
  --api-parent Product \
  --api-field items \
  --api-parent-collection true \
  --build true
```

### 5. Add entities in batch from a model definition file
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar batch-model \
  --project /home/Maciej/projects/onecx/onecx-demo-svc \
  --package org.tkit.onecx.demo \
  --model /home/Maciej/projects/onecx/onecx-svc-generator/generator/examples/model.yaml \
  --build true
``` 

### 6. Build the generated service
```bash
cd /home/Maciej/projects/onecx/onecx-demo-svc
mvn clean package
```

The first build generates REST interfaces and DTOs from OpenAPI using `openapi-generator-maven-plugin`.
The hand-written controllers and mappers already reference those classes and compile after generation.

## Test from repo later

Do **not** commit the built JAR to the repository root.
Recommended flow:

1. build the generator JAR locally,
2. publish it as a GitHub Release asset,
3. run it via the JBang launcher from the repo catalog.

After a release is published:
```bash
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator create-svc --name onecx-demo-svc --group org.tkit.onecx --package org.tkit.onecx.demo
```
