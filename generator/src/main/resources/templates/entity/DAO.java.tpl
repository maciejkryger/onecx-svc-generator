package {{daoPackage}};

import {{generatedModelPackage}}.{{entity}}SearchCriteriaDTO;
import {{modelPackage}}.{{entity}};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.tkit.quarkus.jpa.daos.AbstractDAO;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class {{entity}}DAO extends AbstractDAO<{{entity}}> {

    public List<{{entity}}> findByCriteria({{entity}}SearchCriteriaDTO criteria) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<{{entity}}> cq = cb.createQuery({{entity}}.class);
        Root<{{entity}}> root = cq.from({{entity}}.class);

        List<Predicate> predicates = new ArrayList<>();

{{findByCriteriaPredicates}}

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        TypedQuery<{{entity}}> query = getEntityManager().createQuery(cq);

        int pageNumber = criteria.getPageNumber() != null ? criteria.getPageNumber() : 0;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 100;

        if (pageNumber < 0) {
            pageNumber = 0;
        }
        if (pageSize <= 0) {
            pageSize = 100;
        }

        int offset = pageNumber * pageSize;

        query.setFirstResult(offset);
        query.setMaxResults(pageSize);

        return query.getResultList();
    }
}