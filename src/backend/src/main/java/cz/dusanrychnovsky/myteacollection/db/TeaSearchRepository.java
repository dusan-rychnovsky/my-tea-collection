package cz.dusanrychnovsky.myteacollection.db;

import cz.dusanrychnovsky.myteacollection.model.Availability;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TeaSearchRepository {

  @PersistenceContext
  private EntityManager entityManager;

  public List<Tea> findByCriteria(SearchCriteria criteria) {

    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(Tea.class);
    var tea = query.from(Tea.class);

    var predicates = new ArrayList<Predicate>();

    if (criteria.teaTypeId() != 0) {
      var type = tea.join("types");
      predicates.add(builder.equal(type.get("id"), criteria.teaTypeId()));
    }
    if (criteria.vendorId() != 0) {
      var vendor = tea.join("vendor");
      predicates.add(builder.equal(vendor.get("id"), criteria.vendorId()));
    }
    if (criteria.availabilityId() != 0) {
      var availability = Availability.toBoolean(criteria.availabilityId());
      predicates.add(builder.equal(tea.get("inStock"), availability));
    }
    query.where(predicates.toArray(new Predicate[0]));

    // TODO paginating .setFirstResult(offset).setMaxResults(limit)
    return entityManager.createQuery(query).getResultList();
  }
}
