<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="
            http://www.liquibase.org/xml/ns/dbchangelog
            https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="create-{{tableName}}" author="onecx-svc-generator">
        <createTable tableName="{{tableName}}">
            <column name="id" type="VARCHAR(36)">
                <constraints primaryKey="true"
                             primaryKeyName="pk_{{tableName}}"
                             nullable="false"/>
            </column>

            <column name="tenant" type="VARCHAR(255)"/>

            <column name="creation_user" type="VARCHAR(255)"/>
            <column name="creation_date" type="TIMESTAMP"/>
            <column name="modification_user" type="VARCHAR(255)"/>
            <column name="modification_date" type="TIMESTAMP"/>

{{liquibaseColumns}}
        </createTable>
    </changeSet>

</databaseChangeLog>