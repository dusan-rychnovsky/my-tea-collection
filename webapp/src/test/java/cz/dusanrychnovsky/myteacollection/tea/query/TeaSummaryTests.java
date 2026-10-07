package cz.dusanrychnovsky.myteacollection.tea.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TeaSummaryTests {

  @Test
  void averageRatingLabel_unratedTea_hasNoLabel() {
    assertNull(summary(null).averageRatingLabel());
  }

  @ParameterizedTest
  @CsvSource({
    "0.0, 0.0",
    "1.0, 0.5",
    "8.0, 4.0",
    "8.6, 4.3",
    "8.75, 4.4",
    "9.0, 4.5",
    "10.0, 5.0"
  })
  void averageRatingLabel_formatsStoredHalfStarsOnFiveStarScale(
    double averageHalfStars, String expectedLabel) {

    assertEquals(expectedLabel, summary(averageHalfStars).averageRatingLabel());
  }

  private TeaSummary summary(Double averageHalfStars) {
    return new TeaSummary(
      1L,
      "mei-leaf-luminary-misfit-2022",
      "Luminary Misfit",
      "Lancang Gushu Sheng PuErh Spring 2022",
      "Mei Leaf",
      "Dark Tea, Sheng Puerh",
      "A fruity puerh.",
      averageHalfStars,
      10L,
      List.of()
    );
  }
}
