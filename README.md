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
cd cd ../onecx-svc-generator/generator
mvn clean package -Dquarkus.package.type=uber-jar
```

### 2. Generate a new service
```bash
cd ../
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar create-svc   --name onecx-demo-svc   --group org.tkit.onecx   --package org.tkit.onecx.demo
```
#### with autobuild - recomended for development, as it compiles the generated code after each change:
```bash 
cd ../

java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar create-svc \
  --name onecx-demo-svc \
  --group org.tkit.onecx \
  --package org.tkit.onecx.demo \
  --build true
```

### 3. Add a root entity (creates API + controller + mapper + domain layer)
```bash
cd ../
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
cd ../
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
cd ../
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar batch-model \
  --project /home/Maciej/projects/onecx/onecx-demo-svc \
  --package org.tkit.onecx.demo \
  --model /home/Maciej/projects/onecx/onecx-svc-generator/generator/examples/model.yaml \
  --build true
``` 

#### with Liquibase diff generation for existing entities - generates changelog with missing tables/columns based on the model definition:
```bash
cd ../
java -jar onecx-svc-generator/generator/target/onecx-svc-generator-1.0.0-runner.jar batch-model \
  --project /home/Maciej/projects/onecx/onecx-demo-svc \
  --package org.tkit.onecx.demo \
  --model /home/Maciej/projects/onecx/onecx-svc-generator/generator/examples/model.yaml \
  --liquibase-diff true \
  --build true
``` 

### 6. Build the generated service
```bash
cd ../onecx-demo-svc
mvn clean package
```

The first build generates REST interfaces and DTOs from OpenAPI using `openapi-generator-maven-plugin`.
The hand-written controllers and mappers already reference those classes and compile after generation.

### 7. Run the generated and built service
```bash
cd ../onecx-demo-svc
mvn quarkus:dev
```
### 8. Welcome Quarkus page
http://localhost:8080/q/dev-ui/welcome

### 9. Test locally example model endpoints with curl or Postman:
## internal api:

# create product with category
curl -X POST \
  http://localhost:8080/internal/products \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "name": "Laptop",
    "price": 2999.99,
    "category": {
      "name": "Hardware"
    }
  }'

# get product by id
curl -X GET \
  http://localhost:8080/internal/products/p1a2b3c4-e222-4f66-bbbb-987654321000 \
  -H 'Accept: application/json'

# search products by name
curl -X POST \
  'http://localhost:8080/internal/products/search?limit=20&offset=0' \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "name": "Laptop"
  }'

# delete product by id
curl -X DELETE \
  http://localhost:8080/internal/products/p1a2b3c4-e222-4f66-bbbb-987654321000 \
  -H 'Accept: application/json'



## external API:

# get product by id
curl --request GET \
  --url http://localhost:8080/v1/products/123e4567-e89b-12d3-a456-426614174000 \
  --header 'Accept: application/json'

# search products with pagination
curl --request POST \
--url 'http://localhost:8080/v1/products/search?limit=20&offset=0' \
--header 'Accept: application/json'

# search products by name only
curl --request POST \
  --url 'http://localhost:8080/v1/products/search?limit=20&offset=0' \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "prod"
  }'

# search products by name and price
curl --request POST \
--url 'http://localhost:8080/v1/products/search?limit=20&offset=0' \
--header 'Accept: application/json' \
--header 'Content-Type: application/json' \
--data '{
"name": "test",
"price": 100.00
}'


Do **not** commit the built JAR to the repository root.
Recommended flow:

1. build the generator JAR locally,
2. publish it as a GitHub Release asset,
3. run it via the JBang launcher from the repo catalog.

After a release is published:
```bash
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator create-svc --name onecx-demo-svc --group org.tkit.onecx --package org.tkit.onecx.demo
```
