package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import cz.dusanrychnovsky.myteacollection.tastingnotes.application.ReplaceTeaTastingNotesCommand.NoteData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TastingNoteRecordMapperTests {

  @Test
  void toCommand_carriesTeaAndOwnerIdsAndMapsNotes() {
    var command = TastingNoteRecordMapper.toCommand(7L, 3L, List.of(
      record("4.5", "2026-06-08", List.of("Para one.", "Para two."))));

    assertEquals(7L, command.teaId());
    assertEquals(3L, command.userId());
    assertEquals(1, command.notes().size());
    var note = command.notes().get(0);
    assertEquals(9, note.rating().halfStars());
    assertEquals(LocalDate.of(2026, 6, 8), note.tastedOn());
  }

  @Test
  void toCommand_joinsParagraphsWithBlankLine() {
    var note = onlyNote(record("4.0", "2026-01-01", List.of("First.", "Second.")));
    assertEquals("First.\n\nSecond.", note.body());
  }

  @Test
  void toCommand_trimsParagraphsAndDropsEmpties() {
    var note = onlyNote(record("4.0", "2026-01-01", List.of("  First.  ", "   ", "Second.")));
    assertEquals("First.\n\nSecond.", note.body());
  }

  @Test
  void toCommand_allBlankParagraphs_yieldEmptyBody() {
    // The mapper joins to an empty body; the blank-body invariant is enforced later, by the
    // TastingNote aggregate the use case builds (see TastingNoteTests / ReplaceTeaTastingNotesServiceIT).
    assertEquals("", onlyNote(record("4.0", "2026-01-01", List.of("   ", ""))).body());
  }

  @Test
  void toCommand_invalidRating_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> onlyNote(record("4.3", "2026-01-01", List.of("Body."))));
  }

  private static NoteData onlyNote(TastingNoteRecord record) {
    return TastingNoteRecordMapper.toCommand(1L, 1L, List.of(record)).notes().get(0);
  }

  private static TastingNoteRecord record(String rating, String date, List<String> body) {
    return new TastingNoteRecord(new BigDecimal(rating), LocalDate.parse(date), body);
  }
}
