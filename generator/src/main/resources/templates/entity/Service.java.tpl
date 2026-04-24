package {{domainServicePackage}};

import {{daoPackage}}.{{entity}}DAO;
{{serviceRelationImports}}import {{generatedModelPackage}}.{{entity}}SearchCriteriaDTO;
import {{generatedModelPackage}}.{{generatedDto}};
import {{mapperPackage}}.{{entity}}Mapper;
import {{modelPackage}}.{{entity}};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.NoSuchElementException;

import org.tkit.quarkus.jpa.daos.PageResult;

@ApplicationScoped
public class {{entity}}Service {

    @Inject
    {{entity}}DAO dao;

    @Inject
    {{entity}}Mapper mapper;

{{relationDaoInjections}}
    public PageResult<{{entity}}> findByCriteria({{entity}}SearchCriteriaDTO criteria) {
        return dao.findByCriteria(criteria);
    }

    public {{entity}} findById(String id) {
        {{entity}} entity = dao.findById(id);
        if (entity == null) {
            throw new NoSuchElementException("{{entity}} not found: " + id);
        }
        return entity;
    }

    @Transactional
    public {{entity}} create({{generatedDto}} dto) {
        {{entity}} entity = mapper.fromDto(dto);
{{relationCreateResolvers}}        dao.create(entity);
        return entity;
    }

    @Transactional
    public {{entity}} update(String id, {{generatedDto}} dto) {
        {{entity}} entity = findById(id);
        mapper.update(dto, entity);
{{relationUpdateResolvers}}        return dao.update(entity);
    }

    @Transactional
    public void delete(String id) {
        {{entity}} entity = findById(id);
        dao.delete(entity);
    }
}