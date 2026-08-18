package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static cz.dusanrychnovsky.myteacollection.tastingnotes.ingest.TastingNoteRecord.loadFrom;
import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TastingNoteRecordTests {

  @Test
  void loadFrom_presentFile_parsesRatingDateAndBody() {
    var records = loadFrom(toFile("teas/01")).orElseThrow();

    assertEquals(2, records.size());
    var first = records.get(0);
    assertEquals(new BigDecimal("5.0"), first.rating());
    assertEquals(LocalDate.of(2026, 7, 21), first.date());
    assertEquals(
      List.of("Clean, layered and quietly energizing.", "One of my favourite sessions this year."),
      first.body());
  }

  @Test
  void loadFrom_absentFile_returnsEmpty() {
    assertTrue(loadFrom(toFile("teas/04")).isEmpty());
  }

  @Test
  void loadFrom_emptyArray_returnsEmptyList() {
    assertEquals(List.of(), loadFrom(toFile("teas/03")).orElseThrow());
  }

  @Test
  void loadFrom_missingRequiredField_throws(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve(TastingNoteRecord.FILE_NAME), """
      [ { "date": "2026-01-01", "body": [ "No rating here." ] } ]
      """);
    assertThrows(CannotLoadTastingNotesException.class, () -> loadFrom(dir.toFile()));
  }

  @Test
  void loadFrom_malformedJson_throws(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve(TastingNoteRecord.FILE_NAME), "{ not valid json ");
    assertThrows(CannotLoadTastingNotesException.class, () -> loadFrom(dir.toFile()));
  }

  @Test
  void loadFrom_badDateFormat_throws(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve(TastingNoteRecord.FILE_NAME), """
      [ { "rating": 4.0, "date": "21-07-2026", "body": [ "Bad date." ] } ]
      """);
    assertThrows(CannotLoadTastingNotesException.class, () -> loadFrom(dir.toFile()));
  }
}
