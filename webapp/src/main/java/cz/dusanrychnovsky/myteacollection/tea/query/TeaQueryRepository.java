package cz.dusanrychnovsky.myteacollection.tea.query;

import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

@Repository
public class TeaQueryRepository {

  @PersistenceContext
  private EntityManager entityManager;

  public List<TeaSummary> getPage(
    FilterCriteria filterCriteria, SearchCriteria searchCriteria, int pageNo, int pageSize) {

    var page = fetchPage(filterCriteria, searchCriteria, pageNo, pageSize);
    if (page.isEmpty()) {
      return List.of();
    }

    var teaIds = page.stream().map(row -> row.get("id", Long.class)).toList();
    var mainImageIds = fetchMainImageIds(teaIds);
    var typeNames = fetchTypeNames(teaIds);
    var tags = fetchTags(teaIds);

    return page.stream()
      .map(row -> {
        var id = row.get("id", Long.class);
        return new TeaSummary(
          id,
          row.get("title", String.class),
          row.get("name", String.class),
          row.get("vendorName", String.class),
          typeNames.getOrDefault(id, ""),
          row.get("description", String.class),
          mainImageIds.get(id),
          tags.getOrDefault(id, List.of())
        );
      })
      .toList();
  }

  public long count(FilterCriteria filterCriteria, SearchCriteria searchCriteria) {
    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createQuery(Long.class);
    var teaParent = query.from(TeaEntity.class);
    withPredicates(builder, query, teaParent, filterCriteria, searchCriteria);
    query.select(builder.countDistinct(teaParent));
    return entityManager.createQuery(query).getSingleResult();
  }

  private List<Tuple> fetchPage(
    FilterCriteria filterCriteria, SearchCriteria searchCriteria, int pageNo, int pageSize) {

    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createTupleQuery();
    var teaParent = query.from(TeaEntity.class);

    query.multiselect(
      teaParent.get("id").alias("id"),
      teaParent.get("title").alias("title"),
      teaParent.get("name").alias("name"),
      teaParent.get("description").alias("description"),
      teaParent.get("vendor").get("name").alias("vendorName")
    );
    query.distinct(true);
    query.orderBy(builder.asc(teaParent.get("id")));

    withPredicates(builder, query, teaParent, filterCriteria, searchCriteria);

    var typedQuery = entityManager.createQuery(query);
    typedQuery.setFirstResult(pageNo * pageSize);
    typedQuery.setMaxResults(pageSize);
    return typedQuery.getResultList();
  }

  private Map<Long, Long> fetchMainImageIds(List<Long> teaIds) {
    var builder = entityManager.getCriteriaBuilder();
    var query = builder.createTupleQuery();
    var image = query.from(TeaImageEntity.class);
    query.multiselect(
      image.get("tea").get("id").alias("teaId"),
      image.get("id").alias("imageId"),
      image.get("index").alias("index")
    );
    query.where(image.get("tea").get("id").in(teaIds));

    var bestIndex = new HashMap<Long, Integer>();
    var mainImageIds = new LinkedHashMap<Long, Long>();
    for (var row : entityManager.createQuery(query).getResultList()) {
      var teaId = row.get("teaId", Long.class);
      var imageId = row.get("imageId", Long.class);
      var index = row.get("index", Integer.class);
      var best = bestIndex.get(teaId);
      if (best == null
        || index < best
        || (index.intValue() == best && imageId < mainImageIds.get(teaId))) {
        bestIndex.put(teaId, index);
        mainImageIds.put(teaId, imageId);
      }
    }
    return mainImageIds;
  }

  private Map<Long, String> fetchTypeNames(List<Long> teaIds) {
    var rows = entityManager.createQuery(
        "SELECT t.id AS teaId, ty.id AS typeId, ty.name AS typeName " +
        "FROM TeaEntity t JOIN t.types ty WHERE t.id IN :ids", Tuple.class)
      .setParameter("ids", teaIds)
      .getResultList();

    var byTea = rows.stream().collect(groupingBy(row -> row.get("teaId", Long.class)));

    var typeNames = new LinkedHashMap<Long, String>();
    byTea.forEach((teaId, typeRows) -> typeNames.put(
      teaId,
      typeRows.stream()
        .sorted(comparingLong(row -> row.get("typeId", Long.class)))
        .map(row -> row.get("typeName", String.class))
        .collect(joining(", "))
    ));
    return typeNames;
  }

  private Map<Long, List<TeaTag>> fetchTags(List<Long> teaIds) {
    var rows = entityManager.createQuery(
        "SELECT t.id AS teaId, tag.id AS tagId, tag.label AS label, tag.description AS description " +
        "FROM TeaEntity t JOIN t.tags tag WHERE t.id IN :ids", Tuple.class)
      .setParameter("ids", teaIds)
      .getResultList();

    var byTea = rows.stream().collect(groupingBy(row -> row.get("teaId", Long.class)));

    var tags = new LinkedHashMap<Long, List<TeaTag>>();
    byTea.forEach((teaId, tagRows) -> tags.put(
      teaId,
      tagRows.stream()
        .sorted(comparingLong(row -> row.get("tagId", Long.class)))
        .map(row -> new TeaTag(row.get("label", String.class), row.get("description", String.class)))
        .toList()
    ));
    return tags;
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
