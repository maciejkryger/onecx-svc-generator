# --- quarkus ---
quarkus.banner.enabled=false
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.metrics.enabled=true
quarkus.hibernate-orm.jdbc.timezone=UTC

# --- auth ---
quarkus.http.auth.permission.health.paths=/q/*
quarkus.http.auth.permission.health.policy=permit
quarkus.http.auth.permission.default.paths=/*
quarkus.http.auth.permission.default.policy=authenticated

# --- auth disabled for dev ---
%dev.quarkus.http.auth.permission.default.policy=permit
%dev.quarkus.http.auth.proactive=false
%dev.onecx.permissions.allow-all=true
%dev.quarkus.otel.sdk.disabled=true
%dev.quarkus.oidc-client.discovery-enabled=false
%dev.tkit.security.auth.enabled=false

# --- datasource ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.max-size=30
quarkus.datasource.jdbc.min-size=10
quarkus.datasource.metrics.enabled=true

# --- dev defaults ---
%dev.quarkus.datasource.devservices.db-name={{dbName}}
%dev.quarkus.datasource.devservices.port=5432
%dev.quarkus.datasource.username=onecx-dev
%dev.quarkus.datasource.password=onecx-dev

# --- production defaults ---
%prod.quarkus.oidc-client.client-id=${quarkus.application.name}
%prod.quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://postgresdb:5432/{{dbName}}?sslmode=disable}
%prod.quarkus.datasource.username=${DB_USER:{{name}}}
%prod.quarkus.datasource.password=${DB_PWD:{{name}}}

# --- liquibase ---
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.validate-on-migrate=true