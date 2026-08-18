package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * External JSON contract for a single tasting note in a tea's {@code tasting-notes.json}. The owner
 * authors the body as an array of paragraphs (joined on the write side); the rating is a 0.0–5.0
 * number and the date an ISO {@code yyyy-MM-dd}. All fields are required.
 */
public record TastingNoteRecord(
  @JsonProperty(value = "rating", required = true) BigDecimal rating,
  @JsonProperty(value = "date", required = true) LocalDate date,
  @JsonProperty(value = "body", required = true) List<String> body
) {

  public static final String FILE_NAME = "tasting-notes.json";

  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

  /**
   * Loads a tea directory's tasting notes: an empty {@link Optional} if the file is absent (the tea
   * is skipped and keeps its existing notes), otherwise the — possibly empty — list of records
   * (an empty list clears the tea's notes).
   */
  public static Optional<List<TastingNoteRecord>> loadFrom(File teaDir) {
    var file = new File(teaDir, FILE_NAME);
    if (!file.exists()) {
      return Optional.empty();
    }
    try {
      return Optional.of(MAPPER.readValue(file, new TypeReference<List<TastingNoteRecord>>() {}));
    }
    catch (IOException ex) {
      throw new CannotLoadTastingNotesException(file, ex);
    }
  }
}
