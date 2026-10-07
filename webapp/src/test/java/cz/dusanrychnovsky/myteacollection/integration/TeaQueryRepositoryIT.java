package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.tea.query.FilterCriteria;
import cz.dusanrychnovsky.myteacollection.tea.query.SearchCriteria;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaQueryRepository;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaSort;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaSummary;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaTag;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
  void getPage_newest_projectsFieldsAndOrdersByDescendingId() {
    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, SearchCriteria.EMPTY, TeaSort.NEWEST, 0, 9);

    assertEquals(7, page.size());
    assertEquals(
      List.of("Long Description Fixture", "Approximate Season Fixture"),
      page.stream().limit(2).map(TeaSummary::title).toList()
    );

    var doubleshot = summaryByTitle(page, "Doubleshot");
    assertEquals("meetea-doubleshot-2022", doubleshot.slug());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", doubleshot.name());
    assertEquals("Meetea", doubleshot.vendorName());
    assertEquals("Dark Tea, Shu Puerh", doubleshot.typeNames());
    assertNotNull(doubleshot.description());
    assertNotNull(doubleshot.tags());
    assertEquals(mainImageIdOf("Doubleshot"), doubleshot.mainImageId());
  }

  @Test
  @Transactional
  void getPage_newestSecondPage_returnsNextTeasInDescendingIdOrder() {
    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, SearchCriteria.EMPTY, TeaSort.NEWEST, 1, 2);

    assertEquals(
      List.of("Jade Star 8", "Shou Mei 2017"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void getPage_highestScore_ordersByMaterializedAverageAndPlacesUnratedTeasLast() {
    var doubleshot = teaByTitle("Doubleshot");
    var luminary = teaByTitle("Luminary Misfit");
    var shouMei = teaByTitle("Shou Mei 2017");
    doubleshot.setAverageRatingHalfStars(8.0);
    luminary.setAverageRatingHalfStars(9.0);
    shouMei.setAverageRatingHalfStars(8.0);
    teaRepository.saveAllAndFlush(List.of(doubleshot, luminary, shouMei));

    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, SearchCriteria.EMPTY, TeaSort.HIGHEST_SCORE, 0, 9);

    assertEquals(
      List.of("Luminary Misfit", "Shou Mei 2017", "Doubleshot"),
      page.stream().limit(3).map(TeaSummary::title).toList()
    );
    assertEquals(
      List.of(
        "Long Description Fixture",
        "Approximate Season Fixture",
        "Jade Star 8",
        "Simple Dreams 2"
      ),
      page.stream().skip(3).map(TeaSummary::title).toList()
    );
  }

  @ParameterizedTest
  @EnumSource(TeaSort.class)
  @Transactional
  void getPage_projectsStoredFractionalZeroAndMissingRatings(TeaSort sort) {
    var luminary = teaByTitle("Luminary Misfit").setAverageRatingHalfStars(8.6);
    var doubleshot = teaByTitle("Doubleshot").setAverageRatingHalfStars(0.0);
    teaRepository.saveAllAndFlush(List.of(luminary, doubleshot));

    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, SearchCriteria.EMPTY, sort, 0, 9);

    assertEquals(8.6, summaryByTitle(page, "Luminary Misfit").averageRatingHalfStars());
    assertEquals(0.0, summaryByTitle(page, "Doubleshot").averageRatingHalfStars());
    assertNull(summaryByTitle(page, "Shou Mei 2017").averageRatingHalfStars());
  }

  @Test
  @Transactional
  void count_returnsTotalNumberOfTeas() {
    assertEquals(7, teaQueryRepository.count(FilterCriteria.EMPTY, SearchCriteria.EMPTY));
  }

  @Test
  @Transactional
  void getPage_filterByType_returnsOnlyMatchingSummaries() {
    var page = teaQueryRepository.getPage(
      new FilterCriteria(4, 2, 0), SearchCriteria.EMPTY, TeaSort.NEWEST, 0, 9);

    assertEquals(
      List.of("Shou Mei 2017"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void getPage_search_returnsOnlyMatchingSummaries() {
    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, new SearchCriteria("shou mei"), TeaSort.NEWEST, 0, 9);

    assertEquals(
      List.of("Jade Star 8", "Shou Mei 2017", "Simple Dreams 2"),
      page.stream().map(TeaSummary::title).toList()
    );
  }

  @Test
  @Transactional
  void getPage_filterByInStockAvailability_returnsInStockTeas() {
    var page = teaQueryRepository.getPage(
      new FilterCriteria(0, 0, 1), SearchCriteria.EMPTY, TeaSort.NEWEST, 0, 9);

    assertEquals(7, page.size());
  }

  @Test
  @Transactional
  void getPage_filterByOutOfStockAvailability_returnsEmptyPage() {
    var page = teaQueryRepository.getPage(
      new FilterCriteria(0, 0, 2), SearchCriteria.EMPTY, TeaSort.NEWEST, 0, 9);

    assertTrue(page.isEmpty());
  }

  @Test
  @Transactional
  void getPage_populatesTags() {
    var page = teaQueryRepository.getPage(
      FilterCriteria.EMPTY, SearchCriteria.EMPTY, TeaSort.NEWEST, 0, 9);

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
    return teaByTitle(title).getImages().stream().min(comparingInt(TeaImageEntity::getIndex)).orElseThrow().getId();
  }

  private TeaEntity teaByTitle(String title) {
    return teaRepository.findAll().stream()
      .filter(tea -> tea.getTitle().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB: " + title));
  }
}
