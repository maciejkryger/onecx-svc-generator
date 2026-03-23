# onecx-svc-generator

# Structure
onecx-svc-generator/
├─ jbang-catalog.json                  # alias: onecx-svc-generator
├─ cli/
│  └─ onecx-svc-generator.java         # JBang + Quarkus (Picocli, Qute)
└─ templates/
├─ svc-project/                     # template for OneCX *-svc
│  ├─ pom.xml.tpl                   # parent: onecx-quarkus3-parent
│  ├─ application.properties.tpl    # PostgreSQL, Liquibase, OIDC, MT
│  ├─ Dockerfile.jvm.tpl
│  ├─ Dockerfile.native.tpl
│  ├─ Chart.yaml.tpl                # Helm chart
│  ├─ values.yaml.tpl               # default values for Helm
│  └─ openapi-skeleton.yaml.tpl     # API-first (OpenAPI)
└─ entity/                          # domain entity template
├─ Entity.java.tpl
├─ DAO.java.tpl                  # PanacheRepository
├─ Service.java.tpl
└─ Liquibase-changelog.xml.tpl   # for Liquibase-based schema management


# run locally once:
curl -Ls https://sh.jbang.dev | bash -s - app setup

# help
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator --help

# new service project creation
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator create-svc \
--name onecx-demo-svc \
--group org.tkit.onecx \
--package org.tkit.onecx.demo \
--parent-version 0.72.0

# add new entity to existing service project and generate Liquibase changelog 
# ( adding: src/main/java/.../entity/Product.java, src/main/java/.../dao/ProductDAO.java, src/main/java/.../service/ProductService.java, src/main/resources/db/changelog-product.xml)
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator add-entity \
--project ./onecx-demo-svc \
--package org.tkit.onecx.demo \
--entity Product \
--fields "name:String price:BigDecimal" \
--relations "category:ManyToOne:Category"

# generate model from yaml to create more entities
# model.yaml
entities:
- name: Category
  fields: [name:String]
- name: Product
  fields: [name:String, price:BigDecimal]
  relations: [category:ManyToOne:Category]
# generate entities and Liquibase changelogs from model.yaml
jbang onecx-svc-generator@maciejkryger/onecx-svc-generator batch-model \
  --file model.yaml \
  --project ./onecx-demo-svc \
  --package org.tkit.onecx.demo
