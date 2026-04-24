package {{daoPackage}};

import {{generatedModelPackage}}.{{entity}}SearchCriteriaDTO;
import {{modelPackage}}.{{entity}};
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Predicate;
import org.tkit.quarkus.jpa.daos.AbstractDAO;
import org.tkit.quarkus.jpa.daos.Page;
import org.tkit.quarkus.jpa.daos.PageResult;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class {{entity}}DAO extends AbstractDAO<{{entity}}> {

    public PageResult<{{entity}}> findByCriteria({{entity}}SearchCriteriaDTO criteria) {
        try {
            var cb = getEntityManager().getCriteriaBuilder();
            var cq = cb.createQuery({{entity}}.class);
            var root = cq.from({{entity}}.class);

            List<Predicate> predicates = new ArrayList<>();

{{findByCriteriaPredicates}}
            if (!predicates.isEmpty()) {
                cq.where(cb.and(predicates.toArray(new Predicate[0])));
            }

            int pageNumber = criteria.getPageNumber() != null ? criteria.getPageNumber() : 0;
            int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 100;
            if (pageNumber < 0) pageNumber = 0;
            if (pageSize <= 0) pageSize = 100;

            return createPageQuery(cq, Page.of(pageNumber, pageSize)).getPageResult();
        } catch (Exception ex) {
            throw handleConstraint(ex, ErrorKeys.ERROR_FIND_BY_CRITERIA);
        }
    }

    public enum ErrorKeys {
        ERROR_FIND_BY_CRITERIA
    }
}