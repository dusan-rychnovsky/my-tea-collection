package cz.dusanrychnovsky.myteacollection.db;

import cz.dusanrychnovsky.myteacollection.model.Availability;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TeaSearchRepository {

  @PersistenceContext
  private EntityManager entityManager;

  public List<TeaEntity> findByCriteria(SearchCriteria criteria) {

    var criteriaBuilder = entityManager.getCriteriaBuilder();
    var criteriaQuery = criteriaBuilder.createQuery(TeaEntity.class);
    var teaParent = criteriaQuery.from(TeaEntity.class);

    teaParent.fetch("types", JoinType.INNER);
    teaParent.fetch("vendor", JoinType.INNER);
    teaParent.fetch("images", JoinType.LEFT);
    teaParent.fetch("tags", JoinType.LEFT);

    var predicates = new ArrayList<Predicate>();
    if (criteria.teaTypeId() != 0) {
      var type = teaParent.join("types");
      predicates.add(criteriaBuilder.equal(type.get("id"), criteria.teaTypeId()));
    }
    if (criteria.vendorId() != 0) {
      var vendor = teaParent.join("vendor");
      predicates.add(criteriaBuilder.equal(vendor.get("id"), criteria.vendorId()));
    }
    if (criteria.availabilityId() != 0) {
      var availability = Availability.toBoolean(criteria.availabilityId());
      predicates.add(criteriaBuilder.equal(teaParent.get("inStock"), availability));
    }
    criteriaQuery.where(predicates.toArray(new Predicate[0]));

    return entityManager.createQuery(criteriaQuery).getResultList();
  }
}
