package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.tastingnotes.ingest.TastingNoteRecord;
import cz.dusanrychnovsky.myteacollection.tastingnotes.ingest.CannotLoadTastingNotesException;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.tastingnotes.ingest.UploadTastingNotes;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@ExtendWith(OutputCaptureExtension.class)
class UploadTastingNotesIT {

  private static final String TWO_NOTES = """
    [
      { "rating": 5.0, "date": "2026-07-21", "body": ["Clean and layered.", "A favourite."] },
      { "rating": 4.5, "date": "2026-06-08", "body": ["Thick, syrupy texture."] }
    ]
    """;

  private static final String ONE_NOTE = """
    [ { "rating": 4.0, "date": "2026-05-19", "body": ["Very fragrant."] } ]
    """;

  private static final String INVALID_RATING = """
    [ { "rating": 4.3, "date": "2026-05-19", "body": ["Off-step rating."] } ]
    """;

  private static final String MALFORMED = "{ not valid json ";

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private UploadTastingNotes uploadTastingNotes;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private TastingNoteRepository tastingNoteRepository;

  @Autowired
  private UserRepository userRepository;

  private List<TeaEntity> teas;

  @BeforeEach
  void setup() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
    teas = teaRepository.findAll(Sort.by("id"));
  }

  @Test
  @Transactional
  void run_replacesNotesPerTea_skipsTeasWithoutFile(@TempDir Path root) throws IOException {
    var first = teas.get(0).getId();
    var second = teas.get(1).getId();
    writeNotes(root, first, TWO_NOTES);
    writeNotes(root, second, ONE_NOTE);

    uploadTastingNotes.run(root.toFile());

    var firstNotes = tastingNoteRepository.findByTeaIdNewestFirst(first);
    assertEquals(2, firstNotes.size());
    assertEquals(LocalDate.of(2026, 7, 21), firstNotes.get(0).getTastedOn());
    assertEquals("Clean and layered.\n\nA favourite.", firstNotes.get(0).getBody());
    assertEquals(10, firstNotes.get(0).getRatingHalfStars());
    assertEquals(1, tastingNoteRepository.findByTeaIdNewestFirst(second).size());
    // a tea whose folder was never present keeps no notes
    assertEquals(0, tastingNoteRepository.findByTeaIdNewestFirst(teas.get(2).getId()).size());
  }

  @Test
  @Transactional
  void run_isIdempotent_reRunYieldsOneSetOfNotes(@TempDir Path root) throws IOException {
    var teaId = teas.get(0).getId();
    writeNotes(root, teaId, TWO_NOTES);

    uploadTastingNotes.run(root.toFile());
    uploadTastingNotes.run(root.toFile());

    assertEquals(2, tastingNoteRepository.findByTeaIdNewestFirst(teaId).size());
  }

  @Test
  @Transactional
  void run_replacesPreExistingNotes(@TempDir Path root) throws IOException {
    var teaId = teas.get(0).getId();
    seedNote(teas.get(0), "Old note to be superseded.");
    writeNotes(root, teaId, TWO_NOTES);

    uploadTastingNotes.run(root.toFile());

    var notes = tastingNoteRepository.findByTeaIdNewestFirst(teaId);
    assertEquals(2, notes.size());
    assertTrue(notes.stream().noneMatch(n -> n.getBody().contains("superseded")));
  }

  @Test
  @Transactional
  void run_absentFile_leavesExistingNotesUntouched(@TempDir Path root) throws IOException {
    var teaId = teas.get(0).getId();
    seedNote(teas.get(0), "Kept because the folder has no file.");
    Files.createDirectories(root.resolve(String.valueOf(teaId))); // folder present, file absent

    uploadTastingNotes.run(root.toFile());

    assertEquals(1, tastingNoteRepository.findByTeaIdNewestFirst(teaId).size());
  }

  @Test
  @Transactional
  void run_emptyArray_clearsExistingNotes(@TempDir Path root) throws IOException {
    var teaId = teas.get(0).getId();
    seedNote(teas.get(0), "Cleared by an empty file.");
    writeNotes(root, teaId, "[]");

    uploadTastingNotes.run(root.toFile());

    assertEquals(0, tastingNoteRepository.findByTeaIdNewestFirst(teaId).size());
  }

  @Test
  @Transactional
  void run_invalidNote_abortsAndKeepsExistingNotes(CapturedOutput output, @TempDir Path root)
    throws IOException {
    var teaId = teas.get(0).getId();
    seedNote(teas.get(0), "Kept because the new file is rejected.");
    writeNotes(root, teaId, INVALID_RATING);

    var ex = assertThrows(IllegalArgumentException.class, () -> uploadTastingNotes.run(root.toFile()));

    assertTrue(ex.getMessage().toLowerCase().contains("half"));
    assertTrue(output.getAll().contains("Failed to load tasting notes for tea #" + teaId));
    assertEquals(1, tastingNoteRepository.findByTeaIdNewestFirst(teaId).size());
  }

  @Test
  @Transactional
  void run_malformedJson_throws(@TempDir Path root) throws IOException {
    var teaId = teas.get(0).getId();
    writeNotes(root, teaId, MALFORMED);

    assertThrows(CannotLoadTastingNotesException.class, () -> uploadTastingNotes.run(root.toFile()));
  }

  private void writeNotes(Path root, Long teaId, String json) throws IOException {
    var dir = Files.createDirectories(root.resolve(String.valueOf(teaId)));
    Files.writeString(dir.resolve(TastingNoteRecord.FILE_NAME), json);
  }

  private void seedNote(TeaEntity tea, String body) {
    var owner = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow();
    tastingNoteRepository.save(new TastingNoteEntity(tea, owner, 6, LocalDate.of(2026, 1, 1), body));
  }
}
