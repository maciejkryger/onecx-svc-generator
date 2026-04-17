package {{package}}.rs.external.v1.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import {{modelPackage}}.{{entity}};
import {{generatedExternalModelPackage}}.{{generatedExternalDto}};
import {{generatedExternalModelPackage}}.{{generatedExternalSearchCriteria}};

class {{entity}}MapperTest {

    private final {{entity}}Mapper mapper = new {{entity}}MapperImpl();

    @Test
    void shouldMapEntityToExternalDto() {
        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        {{generatedExternalDto}} dto = mapper.toDto(entity);

        assertNotNull(dto);
        {{testExternalDtoAssertions}}
    }

    @Test
    void shouldMapExternalSearchCriteriaToInternalCriteria() {
        {{testExternalSearchCriteriaBody}}

        var criteria = mapper.toCriteria(request);

        assertNotNull(criteria);
    }
}