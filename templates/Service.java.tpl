package {{package}}.service;

import {{package}}.dao.{{entity}}DAO;
import {{package}}.entity.{{entity}};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.*;

@ApplicationScoped
public class {{entity}}Service {

    @Inject {{entity}}DAO dao;

    public List<{{entity}}> listAll() { return dao.listAll(); }

    @Transactional
    public {{entity}} create({{entity}} e) { dao.persist(e); return e; }

    @Transactional
    public {{entity}} update(Long id, {{entity}} e) {
        {{entity}} cur = dao.findById(id);
        if (cur == null) throw new NoSuchElementException("{{entity}} not found");
        e.setId(id);
        return dao.getEntityManager().merge(e);
    }

    @Transactional
    public void delete(Long id) { dao.deleteById(id); }
}
