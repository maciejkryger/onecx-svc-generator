package {{package}}.domain.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import {{daoPackage}}.{{entity}}DAO;
import {{generatedModelPackage}}.{{generatedDto}};
import {{generatedModelPackage}}.{{generatedInternalSearchCriteria}};
import {{mapperPackage}}.{{entity}}Mapper;
import {{modelPackage}}.{{entity}};
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class {{entity}}ServiceTest {

    @Inject
    {{entity}}Service service;

    @InjectMock
    {{entity}}DAO dao;

    @InjectMock
    {{entity}}Mapper mapper;

    @Test
    void shouldCreate{{entity}}() {
        {{generatedDto}} dto = new {{generatedDto}}();
        {{testDtoFieldsInit}}

        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        when(mapper.fromDto(any({{generatedDto}}.class))).thenReturn(entity);

        {{entity}} result = service.create(dto);

        assertNotNull(result);
    }

    @Test
    void shouldUpdate{{entity}}() {
        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        {{generatedDto}} dto = new {{generatedDto}}();
        {{testDtoUpdateFieldsInit}}

        when(dao.findById(any())).thenReturn(entity);
        when(dao.update(any({{entity}}.class))).thenAnswer(invocation -> invocation.getArgument(0));

        {{entity}} result = service.update("test-id", dto);

        assertNotNull(result);
    }

    @Test
    void shouldFindByCriteria() {
        {{generatedInternalSearchCriteria}} criteria = new {{generatedInternalSearchCriteria}}();
        criteria.setPageNumber(0);
        criteria.setPageSize(10);

        {{entity}} entity = new {{entity}}();
        {{testEntityFieldsInit}}

        when(dao.findByCriteria(any())).thenReturn(List.of(entity));

        var result = service.findByCriteria(criteria);

        assertNotNull(result);
    }
}