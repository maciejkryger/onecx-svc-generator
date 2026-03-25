openapi: 3.0.3
info:
  title: {{name}} API
  version: 1.0.0
servers:
  - url: /api
paths:
  /ping:
    get:
      tags: [ping]
      operationId: ping
      responses:
        '200':
          description: pong
          content:
            text/plain:
              schema:
                type: string
