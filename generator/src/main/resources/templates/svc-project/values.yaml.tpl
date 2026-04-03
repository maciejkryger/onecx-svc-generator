app:
  name: svc
  image:
    repository: "onecx/{{name}}"
  db:
    enabled: true

operator:
  microservice:
    spec:
      name: "{{name}}"
      description: "OneCX backend service"