app:
  name: svc
image:
  repository: "onecx/{{name}}"
  tag: "latest"

db:
  enabled: true

operator:
  keycloak:
    client:
      enabled: true
      spec:
        kcConfig:
          defaultClientScopes: [ ocx-tn:read ]

microservice:
  spec:
    name: {{name}}
    description: OneCX Backend Service
