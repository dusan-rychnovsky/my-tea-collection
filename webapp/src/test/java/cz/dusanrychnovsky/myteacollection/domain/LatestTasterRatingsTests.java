package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
      tasting(2, "2026-01-15", 8),
      tasting(1, "2026-02-01", 10),
      tasting(1, "2026-01-01", 4)
    ));

    assertEquals(2, latest.tasterCount());
    assertEquals(9.0, latest.averageHalfStars().orElseThrow());
    assertTrue(latest.ratings().containsAll(List.of(new Rating(10), new Rating(8))));
  }

  @Test
  void of_selectsLatestDateRegardlessOfInputOrder() {
    var older = tasting(1, "2026-01-01", 4);
    var latest = tasting(1, "2026-02-01", 10);

    for (var notes : List.of(List.of(older, latest), List.of(latest, older))) {
      assertEquals(List.of(new Rating(10)), LatestTasterRatings.of(notes).ratings());
    }
  }

  @Test
  void of_sameDate_arbitrarilyKeepsFirstEncounteredNote() {
    var first = tasting(1, "2026-01-01", 4);
    var other = tasting(1, "2026-01-01", 10);

    assertEquals(List.of(new Rating(4)), LatestTasterRatings.of(List.of(first, other)).ratings());
    assertEquals(List.of(new Rating(10)), LatestTasterRatings.of(List.of(other, first)).ratings());
  }

  @Test
  void of_nullNotes_throws() {
    assertThrows(IllegalArgumentException.class, () -> LatestTasterRatings.of(null));
  }

  @Test
  void of_nullNote_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> LatestTasterRatings.of(Collections.singletonList(null)));
  }

  @Test
  void of_noteWithoutPersistenceIdentity_isAccepted() {
    var note = new TastingNote(100L, 1L, new Rating(8), LocalDate.of(2026, 1, 1), "Good.");
    var latest = LatestTasterRatings.of(List.of(note));
    assertEquals(List.of(new Rating(8)), latest.ratings());
    assertEquals(1, latest.tasterCount());
    assertEquals(8.0, latest.averageHalfStars().orElseThrow());
  }

  @Test
  void of_notesFromDifferentTeas_throws() {
    var first = tasting(1, "2026-01-01", 4);
    var otherTea = new TastingNote(
      200L, 1L, new Rating(10), LocalDate.of(2026, 2, 1), "Different tea.");

    assertThrows(IllegalArgumentException.class,
      () -> LatestTasterRatings.of(List.of(first, otherTea)));
  }

  private static TastingNote tasting(
    long tasterId, String tastedOn, int halfStars) {

    return new TastingNote(
      100L,
      tasterId,
      new Rating(halfStars),
      LocalDate.parse(tastedOn),
      "A tasting note."
    );
  }
}
