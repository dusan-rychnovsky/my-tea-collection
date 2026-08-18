package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RatingSummaryTests {

  @Test
  void of_noNotes_emptyWithoutAverage() {
    var summary = RatingSummary.of(List.of());
    assertEquals(0, summary.count());
    assertFalse(summary.hasNotes());
    assertNull(summary.averageLabel());
    assertEquals("0 tasting notes", summary.countLabel());
    assertTrue(summary.distribution().isEmpty());
  }

  @Test
  void of_singleNote_usesSingularCountLabel() {
    assertEquals("1 tasting note", RatingSummary.of(List.of(note(8))).countLabel());
  }

  @Test
  void of_multipleNotes_usesPluralCountLabel() {
    assertEquals("2 tasting notes", RatingSummary.of(List.of(note(8), note(10))).countLabel());
  }

  @Test
  void of_averageRoundsHalfUpToOneDecimal() {
    // 5.0, 4.5, 4.0, 4.0 -> 4.375 -> "4.4"
    var summary = RatingSummary.of(List.of(note(10), note(9), note(8), note(8)));
    assertTrue(summary.hasNotes());
    assertEquals("4.4", summary.averageLabel());
  }

  @Test
  void of_distributionHasSixRowsFromFiveToZero() {
    var stars = RatingSummary.of(List.of(note(8))).distribution().stream()
      .map(RatingSummary.DistributionRow::stars)
      .toList();
    assertEquals(List.of(5, 4, 3, 2, 1, 0), stars);
  }

  @Test
  void of_bucketsRoundHalfUp_includingZeroAndFiveStar() {
    // half-stars 10->5, 9->5, 8->4, 1->1, 0->0
    var summary = RatingSummary.of(List.of(note(10), note(9), note(8), note(1), note(0)));
    assertEquals(2, count(summary, 5));
    assertEquals(1, count(summary, 4));
    assertEquals(0, count(summary, 3));
    assertEquals(0, count(summary, 2));
    assertEquals(1, count(summary, 1));
    assertEquals(1, count(summary, 0));
  }

  @Test
  void of_percentagesAreCountOverTotal() {
    var summary = RatingSummary.of(List.of(note(10), note(10), note(8), note(8)));
    assertEquals(50, pct(summary, 5));
    assertEquals(50, pct(summary, 4));
    assertEquals(0, pct(summary, 0));
  }

  private static int count(RatingSummary summary, int stars) {
    return row(summary, stars).count();
  }

  private static int pct(RatingSummary summary, int stars) {
    return row(summary, stars).pct();
  }

  private static RatingSummary.DistributionRow row(RatingSummary summary, int stars) {
    return summary.distribution().stream()
      .filter(r -> r.stars() == stars)
      .findFirst()
      .orElseThrow();
  }

  private static TastingNoteEntity note(int halfStars) {
    return new TastingNoteEntity(null, null, halfStars, LocalDate.of(2026, 1, 1), "body");
  }
}
