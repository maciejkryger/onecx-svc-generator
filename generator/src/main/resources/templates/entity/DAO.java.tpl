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

    public {{entity}}DAO() {
        super({{entity}}.class);
    }

    public List<{{entity}}> findByCriteria({{entity}}SearchCriteriaDTO criteria, Integer offset, Integer limit) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<{{entity}}> cq = cb.createQuery({{entity}}.class);
        Root<{{entity}}> root = cq.from({{entity}}.class);

        List<Predicate> predicates = new ArrayList<>();

{{findByCriteriaPredicates}}

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        TypedQuery<{{entity}}> query = getEntityManager().createQuery(cq);

        if (offset != null && offset >= 0) {
            query.setFirstResult(offset);
        }
        if (limit != null && limit > 0) {
            query.setMaxResults(limit);
        }

        return query.getResultList();
    }
}