# OneCX SVC Generator – patch v2

This patch updates the generator to a more OneCX-like, API-first model and changes the generator package namespace to:

```text
org.tkit.onecx.onecxsvcgen
```

## Replace strategy

Replace **only the files contained in this patch ZIP** 1:1.
Do **not** delete the rest of your repository.

## What to replace

- `generator/pom.xml`
- `generator/src/main/resources/application.properties`
- `generator/src/main/java/org/tkit/onecx/onecxsvcgen/model/*`
- `generator/src/main/java/org/tkit/onecx/onecxsvcgen/service/*`
- `generator/src/main/java/org/tkit/onecx/onecxsvcgen/commands/*`
- `generator/src/main/resources/templates/**/*`
- root `README.md` (optional but recommended)

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

### 3. Add a root entity (creates API + controller + mapper + domain layer)
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar add-entity   --project /home/Maciej/projects/onecx/onecx-demo-svc   --package org.tkit.onecx.demo   --entity Product   --fields name:String,price:BigDecimal   --root true
```

### 4. Add a child entity/component (updates existing API schema, no standalone CRUD)
```bash
cd /home/Maciej/projects/onecx
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar add-entity   --project /home/Maciej/projects/onecx/onecx-demo-svc   --package org.tkit.onecx.demo   --entity ProductItem   --fields quantity:Integer,position:Integer   --root false   --api-parent Product   --api-field items   --api-parent-collection true
```

### 5. Build the generated service
```bash
cd /home/Maciej/projects/onecx/onecx-demo-svc
mvn clean compile
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
jbang onecx-svc-generator@maciejkryger create-svc --name onecx-demo-svc --group org.tkit.onecx --package org.tkit.onecx.demo
```
