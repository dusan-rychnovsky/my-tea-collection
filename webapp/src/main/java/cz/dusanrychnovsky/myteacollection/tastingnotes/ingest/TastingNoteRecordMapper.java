package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import cz.dusanrychnovsky.myteacollection.tastingnotes.application.ReplaceTeaTastingNotesCommand;
import cz.dusanrychnovsky.myteacollection.tastingnotes.application.ReplaceTeaTastingNotesCommand.NoteData;
import cz.dusanrychnovsky.myteacollection.domain.Rating;

import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Ingest anti-corruption layer: translates a tea's external {@link TastingNoteRecord}s into a
 * {@link ReplaceTeaTastingNotesCommand}, parsing each rating into a {@link Rating} value object
 * and joining the body paragraphs (trimmed, blanks dropped) into a single blank-line-separated string.
 * The {@code TastingNote} aggregates themselves — and their invariants — are built by the use case
 * from this command's data.
 */
public final class TastingNoteRecordMapper {

  private static final String PARAGRAPH_SEPARATOR = "\n\n";

  private TastingNoteRecordMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static ReplaceTeaTastingNotesCommand toCommand(
    long teaId, long userId, List<TastingNoteRecord> records) {

    var notes = records.stream().map(TastingNoteRecordMapper::toNoteData).toList();
    return new ReplaceTeaTastingNotesCommand(teaId, userId, notes);
  }

  private static NoteData toNoteData(TastingNoteRecord record) {
    return new NoteData(Rating.ofStars(record.rating()), record.date(), joinBody(record.body()));
  }

  private static String joinBody(List<String> paragraphs) {
    if (paragraphs == null) {
      return "";
    }
    return paragraphs.stream()
      .map(String::trim)
      .filter(paragraph -> !paragraph.isEmpty())
      .collect(joining(PARAGRAPH_SEPARATOR));
  }
}
