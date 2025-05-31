package cz.dusanrychnovsky.myteacollection.db;

import cz.dusanrychnovsky.myteacollection.model.Availability;
import cz.dusanrychnovsky.myteacollection.model.FilterCriteria;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TeaSearchRepository {

  @PersistenceContext
  private EntityManager entityManager;

  public List<TeaEntity> filter(
    FilterCriteria filterCriteria, SearchCriteria searchCriteria) {

    var cb = entityManager.getCriteriaBuilder();
    var cq = cb.createQuery(TeaEntity.class);
    var teaParent = cq.from(TeaEntity.class);

    teaParent.fetch("types", JoinType.INNER);
    teaParent.fetch("vendor", JoinType.INNER);
    teaParent.fetch("images", JoinType.LEFT);
    teaParent.fetch("tags", JoinType.LEFT);

    var predicates = new ArrayList<Predicate>();
    if (filterCriteria.teaTypeId() != 0) {
      var type = teaParent.join("types");
      predicates.add(cb.equal(type.get("id"), filterCriteria.teaTypeId()));
    }
    if (filterCriteria.vendorId() != 0) {
      var vendor = teaParent.join("vendor");
      predicates.add(cb.equal(vendor.get("id"), filterCriteria.vendorId()));
    }
    if (filterCriteria.availabilityId() != 0) {
      var availability = Availability.toBoolean(filterCriteria.availabilityId());
      predicates.add(cb.equal(teaParent.get("inStock"), availability));
    }

    var query = searchCriteria.query();
    if (query != null && !query.isEmpty()) {
      var pattern = "%" + query.toLowerCase() + "%";
      predicates.add(cb.or(
        like(cb, teaParent, "title", pattern),
        like(cb, teaParent, "name", pattern),
        like(cb, teaParent, "description", pattern),
        like(cb, teaParent.get("scope"), "origin", pattern)
      ));
    }

    cq.where(predicates.toArray(new Predicate[0]));

    return entityManager.createQuery(cq).getResultList();
  }

  private Predicate like(CriteriaBuilder cb, Path<TeaEntity> teaParent, String fieldName, String pattern) {
    return cb.like(cb.lower(teaParent.get(fieldName)), pattern);
  }
}
