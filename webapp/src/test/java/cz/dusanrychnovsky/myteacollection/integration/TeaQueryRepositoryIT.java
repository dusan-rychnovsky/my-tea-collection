package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.model.FilterCriteria;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import cz.dusanrychnovsky.myteacollection.query.TeaQueryRepository;
import cz.dusanrychnovsky.myteacollection.query.TeaSummary;
import cz.dusanrychnovsky.myteacollection.query.TeaTag;
import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class TeaQueryRepositoryIT {

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private TeaQueryRepository teaQueryRepository;

  @Autowired
  private TeaRepository teaRepository;

  @BeforeEach
  void setup() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
  }

  @Test
  @Transactional
  void getPage_firstPage_projectsFieldsAndOrdersById() {
    var page = teaQueryRepository.getPage(FilterCriteria.EMPTY, SearchCriteria.EMPTY, 0, 2);

    assertEquals(2, page.size());

    var first = page.get(0);
    assertEquals("Doubleshot", first.title());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", first.name());
    assertEquals("Meetea", first.vendorName());
    assertEquals("Dark Tea, Shu Puerh", first.typeLabels());
    assertNotNull(first.description());
    assertNotNull(first.tags());
    assertEquals(mainImageIdOf("Doubleshot"), first.mainImageId());

    assertEquals("Luminary Misfit", page.get(1).title());
    assertEquals("Dark Tea, Sheng Puerh", page.get(1).typeLabels());
  }

  @Test
  @Transactional
  void getPage_secondPage_returnsNextTeasInIdOrder() {
    var page = teaQueryRepository.getPage(FilterCriteria.EMPTY, SearchCriteria.EMPTY, 1, 2);

    assertEquals(
      List.of("Simple Dreams 2", "Shou Mei 2017"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void count_returnsTotalNumberOfTeas() {
    assertEquals(5, teaQueryRepository.count(FilterCriteria.EMPTY, SearchCriteria.EMPTY));
  }

  @Test
  @Transactional
  void getPage_filterByType_returnsOnlyMatchingSummaries() {
    var page = teaQueryRepository.getPage(new FilterCriteria(4, 2, 0), SearchCriteria.EMPTY, 0, 9);

    assertEquals(
      List.of("Shou Mei 2017"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void getPage_search_returnsOnlyMatchingSummaries() {
    var page = teaQueryRepository.getPage(FilterCriteria.EMPTY, new SearchCriteria("shou mei"), 0, 9);

    assertEquals(
      List.of("Simple Dreams 2", "Shou Mei 2017", "Jade Star 8"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void getPage_filterByInStockAvailability_returnsInStockTeas() {
    var page = teaQueryRepository.getPage(new FilterCriteria(0, 0, 1), SearchCriteria.EMPTY, 0, 9);

    assertEquals(5, page.size());
  }

  @Test
  @Transactional
  void getPage_filterByOutOfStockAvailability_returnsEmptyPage() {
    var page = teaQueryRepository.getPage(new FilterCriteria(0, 0, 2), SearchCriteria.EMPTY, 0, 9);

    assertTrue(page.isEmpty());
  }

  @Test
  @Transactional
  void getPage_populatesTags() {
    var page = teaQueryRepository.getPage(FilterCriteria.EMPTY, SearchCriteria.EMPTY, 0, 9);

    var doubleshot = summaryByTitle(page, "Doubleshot");
    assertEquals(
      Set.of("meetea-2025-jan", "meetea-2024-dec"),
      doubleshot.tags().stream().map(TeaTag::label).collect(toSet())
    );

    assertTrue(summaryByTitle(page, "Luminary Misfit").tags().isEmpty());
  }

  private TeaSummary summaryByTitle(List<TeaSummary> page, String title) {
    return page.stream()
      .filter(summary -> summary.title().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Summary not found: " + title));
  }

  private Long mainImageIdOf(String title) {
    return teaByTitle(title).getMainImage().orElseThrow().getId();
  }

  private TeaEntity teaByTitle(String title) {
    return teaRepository.findAll().stream()
      .filter(tea -> tea.getTitle().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB: " + title));
  }
}
