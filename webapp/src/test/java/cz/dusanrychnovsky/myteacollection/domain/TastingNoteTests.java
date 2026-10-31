package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;

class TastingNoteTests {

  private static final Long TEA_ID = 1L;
  private static final Long TASTER_ID = 2L;
  private static final Rating RATING = new Rating(8);
  private static final LocalDate DATE = LocalDate.of(2026, 5, 19);

  @Test
  void construct_validNote_exposesFields() {
    var note = new TastingNote(TEA_ID, TASTER_ID, RATING, DATE, "A fine cup.");
    assertEquals(TEA_ID, note.getTeaId());
    assertEquals(TASTER_ID, note.getTasterId());
    assertSame(RATING, note.getRating());
    assertEquals(DATE, note.getTastedOn());
    assertEquals("A fine cup.", note.getBody());
  }

  @Test
  void construct_nullTea_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(null, TASTER_ID, RATING, DATE, "Body."));
  }

  @Test
  void construct_nullTaster_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(TEA_ID, null, RATING, DATE, "Body."));
  }

  @Test
  void construct_nullRating_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(TEA_ID, TASTER_ID, null, DATE, "Body."));
  }

  @Test
  void construct_nullDate_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(TEA_ID, TASTER_ID, RATING, null, "Body."));
  }

  @Test
  void construct_nullBody_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(TEA_ID, TASTER_ID, RATING, DATE, null));
  }

  @Test
  void construct_blankBody_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> new TastingNote(TEA_ID, TASTER_ID, RATING, DATE, "   "));
  }
}
