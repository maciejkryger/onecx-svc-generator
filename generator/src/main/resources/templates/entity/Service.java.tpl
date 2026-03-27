package {{package}}.service;

import {{package}}.dao.{{entity}}DAO;
import {{package}}.entity.{{entity}};
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

    @Transactional
    public {{entity}} create({{entity}} entity) {
        dao.persist(entity);
        return entity;
    }

    @Transactional
    public {{entity}} update(Long id, {{entity}} entity) {
        {{entity}} current = dao.findById(id);
        if (current == null) {
            throw new NoSuchElementException("{{entity}} not found: " + id);
        }
        entity.setId(id);
        return dao.getEntityManager().merge(entity);
    }

    @Transactional
    public void delete(Long id) {
        dao.deleteById(id);
    }
}
