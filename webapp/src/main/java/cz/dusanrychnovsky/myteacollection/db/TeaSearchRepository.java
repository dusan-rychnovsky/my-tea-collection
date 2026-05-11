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

  public List<TeaEntity> getPage(
    FilterCriteria filterCriteria, SearchCriteria searchCriteria, int pageNo, int pageSize) {

    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(TeaEntity.class);
    var teaParent = query.from(TeaEntity.class);

    teaParent.fetch("types", JoinType.INNER);
    teaParent.fetch("vendor", JoinType.INNER);
    teaParent.fetch("images", JoinType.LEFT);
    teaParent.fetch("tags", JoinType.LEFT);

    withPredicates(builder, query, teaParent, filterCriteria, searchCriteria);

    var typedQuery = entityManager.createQuery(query);
    typedQuery.setFirstResult(pageNo * pageSize);
    typedQuery.setMaxResults(pageSize);

    return typedQuery.getResultList();
  }

  public long count(FilterCriteria filterCriteria, SearchCriteria searchCriteria) {
    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(Long.class);
    var teaParent = query.from(TeaEntity.class);
    withPredicates(builder, query, teaParent, filterCriteria, searchCriteria);
    query.select(builder.countDistinct(teaParent));
    return entityManager.createQuery(query).getSingleResult();
  }

  private <T> void withPredicates(
    CriteriaBuilder builder, CriteriaQuery<T> query, Root<TeaEntity> teaParent,
    FilterCriteria filterCriteria, SearchCriteria searchCriteria) {

    var predicates = new ArrayList<Predicate>();
    if (filterCriteria.teaTypeId() != 0) {
      var type = teaParent.join("types");
      predicates.add(builder.equal(type.get("id"), filterCriteria.teaTypeId()));
    }
    if (filterCriteria.vendorId() != 0) {
      var vendor = teaParent.join("vendor");
      predicates.add(builder.equal(vendor.get("id"), filterCriteria.vendorId()));
    }
    if (filterCriteria.availabilityId() != 0) {
      var availability = Availability.toBoolean(filterCriteria.availabilityId());
      predicates.add(builder.equal(teaParent.get("inStock"), availability));
    }

    var searchQuery = searchCriteria.query();
    if (searchQuery != null && !searchQuery.isEmpty()) {
      var pattern = "%" + searchQuery.toLowerCase() + "%";
      predicates.add(builder.or(
        like(builder, teaParent, "title", pattern),
        like(builder, teaParent, "name", pattern),
        like(builder, teaParent, "description", pattern),
        like(builder, teaParent.get("scope"), "origin", pattern)
      ));
    }

    query.where(predicates.toArray(new Predicate[0]));
  }

  private Predicate like(CriteriaBuilder cb, Path<TeaEntity> teaParent, String fieldName, String pattern) {
    return cb.like(cb.lower(teaParent.get(fieldName)), pattern);
  }
}
