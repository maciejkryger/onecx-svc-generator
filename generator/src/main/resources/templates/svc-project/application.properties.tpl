# --- datasource ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.max-size=30
quarkus.datasource.jdbc.min-size=10
quarkus.datasource.metrics.enabled=true

# --- auth ---
quarkus.http.auth.permission.health.paths=/q/*
quarkus.http.auth.permission.health.policy=permit
quarkus.http.auth.permission.default.paths=/*
quarkus.http.auth.permission.default.policy=authenticated

# --- auth disabled for dev ---
%dev.quarkus.http.auth.permission.default.policy=permit
%dev.quarkus.http.auth.proactive=false
%dev.quarkus.oidc.tenant-enabled=false
%dev.onecx.permissions.allow-all=true
%dev.quarkus.otel.sdk.disabled=true

# --- quarkus ---
quarkus.banner.enabled=false
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.multitenant=DISCRIMINATOR
quarkus.hibernate-orm.metrics.enabled=true
quarkus.hibernate-orm.jdbc.timezone=UTC

# --- liquibase ---
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.validate-on-migrate=true

# --- production defaults ---
%prod.quarkus.oidc-client.client-id=${quarkus.application.name}
%prod.quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://postgresdb:5432/{{name}}?sslmode=disable}
%prod.quarkus.datasource.username=${DB_USER:{{name}}}
%prod.quarkus.datasource.password=${DB_PWD:{{name}}}
