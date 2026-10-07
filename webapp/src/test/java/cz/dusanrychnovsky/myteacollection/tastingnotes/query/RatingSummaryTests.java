package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RatingSummaryTests {

  @Test
  void of_noNotes_emptyWithoutAverage() {
    var summary = RatingSummary.of(null, List.of());
    assertEquals(0, summary.count());
    assertFalse(summary.hasNotes());
    assertNull(summary.averageLabel());
    assertEquals("0 tasters", summary.averageCountLabel());
    assertEquals("0 tasting notes", summary.countLabel());
    assertTrue(summary.distribution().isEmpty());
  }

  @Test
  void of_noNotesWithMaterializedAverage_throws() {
    assertThrows(IllegalStateException.class, () -> RatingSummary.of(8.0, List.of()));
  }

  @Test
  void of_notesWithoutMaterializedAverage_throws() {
    assertThrows(
      IllegalStateException.class,
      () -> RatingSummary.of(null, List.of(note(1, 1, 8, LocalDate.of(2026, 1, 1)))));
  }

  @Test
  void of_singleNote_usesSingularCountLabel() {
    var notes = List.of(note(1, 1, 8, LocalDate.of(2026, 1, 1)));
    assertEquals("1 tasting note", RatingSummary.of(8.0, notes).countLabel());
  }

  @Test
  void of_multipleHistoricalNotes_usesPluralCountLabel() {
    var notes = List.of(
      note(1, 1, 8, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 2, 1))
    );
    assertEquals("2 tasting notes", RatingSummary.of(10.0, notes).countLabel());
  }

  @Test
  void of_averageLabelUsesMaterializedValueWithoutRecalculation() {
    var notes = List.of(note(1, 1, 10, LocalDate.of(2026, 1, 1)));

    assertEquals("3.0", RatingSummary.of(6.0, notes).averageLabel());
  }

  @Test
  void of_averageLabelRoundsHalfUpToOneDecimal() {
    var notes = List.of(note(1, 1, 8, LocalDate.of(2026, 1, 1)));

    assertEquals("4.4", RatingSummary.of(8.75, notes).averageLabel());
  }

  @Test
  void of_tasterCountCountsOneLatestRatingPerTaster() {
    var notes = List.of(
      note(1, 1, 4, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 2, 1)),
      note(3, 2, 8, LocalDate.of(2026, 1, 15))
    );

    assertEquals("2 tasters", RatingSummary.of(9.0, notes).averageCountLabel());
  }

  @Test
  void of_distributionHasSixRowsFromFiveToZero() {
    var notes = List.of(note(1, 1, 8, LocalDate.of(2026, 1, 1)));
    var stars = RatingSummary.of(8.0, notes).distribution().stream()
      .map(RatingSummary.DistributionRow::stars)
      .toList();
    assertEquals(List.of(5, 4, 3, 2, 1, 0), stars);
  }

  @Test
  void of_distributionUsesOnlyEachTastersLatestRating() {
    var notes = List.of(
      note(1, 1, 0, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 2, 1)),
      note(3, 2, 8, LocalDate.of(2026, 1, 15)),
      note(4, 2, 6, LocalDate.of(2026, 2, 15))
    );

    var summary = RatingSummary.of(8.0, notes);
    assertEquals(1, count(summary, 5));
    assertEquals(1, count(summary, 3));
    assertEquals(0, count(summary, 4));
    assertEquals(0, count(summary, 0));
    assertEquals(50, pct(summary, 5));
    assertEquals(50, pct(summary, 3));
  }

  @Test
  void of_distributionUsesRatingRecordedLaterWhenTasterAndDateMatch() {
    var notes = List.of(
      note(1, 1, 4, LocalDate.of(2026, 1, 1)),
      note(2, 1, 10, LocalDate.of(2026, 1, 1))
    );

    var summary = RatingSummary.of(10.0, notes);
    assertEquals(1, count(summary, 5));
    assertEquals(0, count(summary, 2));
  }

  @Test
  void of_distributionRoundsHalfUpIncludingZeroAndFiveStar() {
    var notes = List.of(
      note(1, 1, 10, LocalDate.of(2026, 1, 1)),
      note(2, 2, 9, LocalDate.of(2026, 1, 1)),
      note(3, 3, 8, LocalDate.of(2026, 1, 1)),
      note(4, 4, 1, LocalDate.of(2026, 1, 1)),
      note(5, 5, 0, LocalDate.of(2026, 1, 1))
    );

    var summary = RatingSummary.of(5.6, notes);
    assertEquals(2, count(summary, 5));
    assertEquals(1, count(summary, 4));
    assertEquals(0, count(summary, 3));
    assertEquals(0, count(summary, 2));
    assertEquals(1, count(summary, 1));
    assertEquals(1, count(summary, 0));
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
