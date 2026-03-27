package {{domainServicePackage}};

import {{daoPackage}}.{{entity}}DAO;
import {{modelPackage}}.{{entity}};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@ApplicationScoped
public class {{entity}}Service {

    @Inject
    {{entity}}DAO dao;

    public List<{{entity}}> listAll() {
        return dao.listAll();
    }

    public {{entity}} findById(Long id) {
        {{entity}} entity = dao.findById(id);
        if (entity == null) {
            throw new NoSuchElementException("{{entity}} not found: " + id);
        }
        return entity;
    }

    @Transactional
    public {{entity}} create({{entity}} entity) {
        dao.persist(entity);
        return entity;
    }

    @Transactional
    public {{entity}} update(Long id, {{entity}} entity) {
        findById(id);
        entity.setId(id);
        return dao.getEntityManager().merge(entity);
    }

    @Transactional
    public void delete(Long id) {
        dao.deleteById(id);
    }
}
