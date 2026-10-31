package cz.dusanrychnovsky.myteacollection.tastingnotes.application;

import cz.dusanrychnovsky.myteacollection.domain.Rating;

import java.time.LocalDate;
import java.util.List;

/**
 * Input to the {@link ReplaceTeaTastingNotes} use case: the target tea, the taster's user id and the full
 * new set of notes for that tea. The operation is a replace — these notes become the tea's complete
 * list, superseding any existing ones. The service combines those intrinsic references with each
 * {@link NoteData} to build and validate a {@code TastingNote}, then resolves the referenced entities.
 */
public record ReplaceTeaTastingNotesCommand(
  Long teaId,
  Long userId,
  List<NoteData> notes
) {

  /**
   * The data for one tasting note: its {@link Rating}, the date it was tasted and its already-joined
   * body text. The {@code TastingNote} aggregate — and its invariants — is built from this by the
   * use case (the aggregate constructor rejects a null date or blank body).
   */
  public record NoteData(Rating rating, LocalDate tastedOn, String body) {
  }
}
