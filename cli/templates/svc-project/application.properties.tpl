# --- DataSource (PostgreSQL) ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.max-size=30
quarkus.datasource.jdbc.min-size=10
quarkus.datasource.metrics.enabled=true

# --- Auth ---
quarkus.http.auth.permission.health.paths=/q/*
quarkus.http.auth.permission.health.policy=permit
quarkus.http.auth.permission.default.paths=/*
quarkus.http.auth.permission.default.policy=authenticated

# --- Quarkus ---
quarkus.banner.enabled=false

# --- Hibernate ---
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.multitenant=DISCRIMINATOR
quarkus.hibernate-orm.metrics.enabled=true
quarkus.hibernate-orm.jdbc.timezone=UTC

# --- Liquibase ---
quarkus.liquibase.migrate-at-start=true
quarkus.liquibase.validate-on-migrate=true

# --- Tenancy / context ---
tkit.rs.context.tenant-id.enabled=true

# --- PROD profile ---
%prod.quarkus.oidc-client.client-id=${quarkus.application.name}
%prod.quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://postgresdb:5432/{{name}}?sslmode=disable}
%prod.quarkus.datasource.username=${DB_USER:{{name}}}
%prod.quarkus.datasource.password=${DB_PWD:{{name}}}
