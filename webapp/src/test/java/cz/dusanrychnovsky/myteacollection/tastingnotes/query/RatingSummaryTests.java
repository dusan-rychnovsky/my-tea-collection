package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RatingSummaryTests {

  @Test
  void of_noNotes_emptyWithoutAverage() {
    var summary = RatingSummary.of(List.of());
    assertEquals(0, summary.count());
    assertFalse(summary.hasNotes());
    assertNull(summary.averageLabel());
    assertEquals("0 tasters", summary.averageCountLabel());
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
    var summary = RatingSummary.of(List.of(
      note(1, 1, 10, LocalDate.of(2026, 1, 1)),
      note(2, 2, 9, LocalDate.of(2026, 1, 1)),
      note(3, 3, 8, LocalDate.of(2026, 1, 1)),
      note(4, 4, 8, LocalDate.of(2026, 1, 1))));
    assertTrue(summary.hasNotes());
    assertEquals("4.4", summary.averageLabel());
    assertEquals("4 tasters", summary.averageCountLabel());
  }

  @Test
  void of_averageUsesLatestRatingFromEachTaster() {
    var summary = RatingSummary.of(List.of(
      note(1, 1, 4, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 2, 1)),
      note(3, 2, 8, LocalDate.of(2026, 1, 15))));

    assertEquals("4.5", summary.averageLabel());
    assertEquals("2 tasters", summary.averageCountLabel());
  }

  @Test
  void of_averageUsesHigherIdForSameTasterAndDate() {
    var summary = RatingSummary.of(List.of(
      note(1, 1, 4, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 1, 1))));

    assertEquals("5.0", summary.averageLabel());
    assertEquals("1 taster", summary.averageCountLabel());
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
    return note(halfStars, halfStars, halfStars, LocalDate.of(2026, 1, 1));
  }

  private static TastingNoteEntity note(
    long noteId, long userId, int halfStars, LocalDate tastedOn) {

    var user = mock(UserEntity.class);
    when(user.getId()).thenReturn(userId);
    var note = mock(TastingNoteEntity.class);
    when(note.getId()).thenReturn(noteId);
    when(note.getUser()).thenReturn(user);
    when(note.getRatingHalfStars()).thenReturn(halfStars);
    when(note.getTastedOn()).thenReturn(tastedOn);
    return note;
  }
}
