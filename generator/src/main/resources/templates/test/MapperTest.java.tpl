package {{package}}.rs.internal.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import {{modelPackage}}.{{entity}};
import {{generatedModelPackage}}.{{generatedDto}};
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class {{entity}}MapperTest {

    @Inject
    {{entity}}Mapper mapper;

    @Test
    void shouldMapEntityToDto() {
        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        {{generatedDto}} dto = mapper.toDto(entity);

        assertNotNull(dto);
        {{testDtoAssertions}}
    }

    @Test
    void shouldMapDtoToEntity() {
        {{generatedDto}} dto = new {{generatedDto}}();
        {{testDtoFieldsInit}}

        {{entity}} entity = mapper.fromDto(dto);

        assertNotNull(entity);
        {{testEntityAssertions}}
    }

    @Test
    void shouldUpdateEntityFromDto() {
        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        {{generatedDto}} dto = new {{generatedDto}}();
        {{testDtoUpdateFieldsInit}}

        mapper.update(dto, entity);

        {{testUpdatedEntityAssertions}}
    }
}