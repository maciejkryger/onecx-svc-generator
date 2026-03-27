app:
  name: svc
image:
  repository: "onecx/{{name}}"
  tag: "latest"

db:
  enabled: true

microservice:
  spec:
    name: {{name}}
    description: OneCX Backend Service
