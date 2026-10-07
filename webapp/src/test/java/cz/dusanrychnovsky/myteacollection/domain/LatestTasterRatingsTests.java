package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestTasterRatingsTests {

  @Test
  void of_noTastings_hasNoRatingsOrAverage() {
    var latest = LatestTasterRatings.of(List.of());

    assertTrue(latest.ratings().isEmpty());
    assertEquals(0, latest.tasterCount());
    assertTrue(latest.averageHalfStars().isEmpty());
  }

  @Test
  void of_selectsEachTastersLatestRating() {
    var latest = LatestTasterRatings.of(List.of(
      tasting(1, 1, "2026-01-01", 4),
      tasting(1, 2, "2026-02-01", 10),
      tasting(2, 3, "2026-01-15", 8)
    ));

    assertEquals(2, latest.tasterCount());
    assertEquals(9.0, latest.averageHalfStars().orElseThrow());
    assertTrue(latest.ratings().containsAll(List.of(new Rating(10), new Rating(8))));
  }

  @Test
  void of_sameDate_selectsRatingRecordedLater() {
    var latest = LatestTasterRatings.of(List.of(
      tasting(1, 1, "2026-01-01", 4),
      tasting(1, 2, "2026-01-01", 10)
    ));

    assertEquals(List.of(new Rating(10)), latest.ratings());
  }

  private static LatestTasterRatings.RatedTasting tasting(
    long tasterId, long recordedOrder, String tastedOn, int halfStars) {

    return new LatestTasterRatings.RatedTasting(
      tasterId,
      recordedOrder,
      LocalDate.parse(tastedOn),
      new Rating(halfStars)
    );
  }
}
