package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaTitleYearTests {

  @Test
  void suffixFor_titleWithoutYearAndExactSeasonYear_returnsSeasonYear() {
    assertEquals(
      Optional.of(2022),
      TeaTitleYear.suffixFor("Luminary Misfit", season("Spring 2022")));
  }

  @Test
  void suffixFor_absentOrApproximateSeason_returnsEmpty() {
    assertTrue(TeaTitleYear.suffixFor("Tea", Optional.empty()).isEmpty());
    assertTrue(TeaTitleYear.suffixFor("Tea", season("Early 2000s")).isEmpty());
  }

  @Test
  void suffixFor_sameSingleTitleAndSeasonYear_returnsEmpty() {
    assertTrue(TeaTitleYear.suffixFor("Shou Mei 2017", season("Spring 2017")).isEmpty());
  }

  @Test
  void suffixFor_differentSingleTitleAndSeasonYears_throwsWithBothValues() {
    var exception = assertThrows(
      IllegalArgumentException.class,
      () -> TeaTitleYear.suffixFor("Tea 2021", season("Spring 2022")));

    assertTrue(exception.getMessage().contains("title year 2021"));
    assertTrue(exception.getMessage().contains("season year 2022"));
  }

  @Test
  void suffixFor_multipleDistinctTitleYears_returnsEmpty() {
    assertTrue(TeaTitleYear.suffixFor("Tea 2020-2021", season("2022")).isEmpty());
  }

  @Test
  void suffixFor_repeatedSameTitleYear_comparesAsSingleYear() {
    assertTrue(TeaTitleYear.suffixFor("2022 Spring / 2022 Autumn", season("2022")).isEmpty());
    assertThrows(
      IllegalArgumentException.class,
      () -> TeaTitleYear.suffixFor("2022 Spring / 2022 Autumn", season("2023")));
  }

  @Test
  void suffixFor_nonYearTitleValues_returnsSeasonYear() {
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Tea 1899", season("2022")));
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Tea 2100", season("2022")));
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Tea 8582", season("2022")));
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Tea 1990s", season("2022")));
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Spring2021 Tea", season("2022")));
    assertEquals(Optional.of(2022), TeaTitleYear.suffixFor("Tea 2021abc", season("2022")));
  }

  private static Optional<Season> season(String value) {
    return Optional.of(new Season(value));
  }
}