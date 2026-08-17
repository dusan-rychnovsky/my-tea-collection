package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.application.ReplaceTeaTastingNotes;
import cz.dusanrychnovsky.myteacollection.application.ReplaceTeaTastingNotesCommand;
import cz.dusanrychnovsky.myteacollection.application.ReplaceTeaTastingNotesCommand.NoteData;
import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link ReplaceTeaTastingNotes} directly (the application-service seam, mirroring
 * {@code AddTeaServiceIT}) to verify its resolve-and-replace contract: it persists a tea's notes,
 * a second call supersedes the first, and unknown tea/owner ids are rejected.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ReplaceTeaTastingNotesServiceIT {

  @Autowired
  private ReplaceTeaTastingNotes replaceTeaTastingNotes;

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private TastingNoteRepository tastingNoteRepository;

  private Long userId;
  private Long teaId;

  @BeforeEach
  void setup() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
    userId = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow().getId();
    teaId = teaRepository.findAll(Sort.by("id")).get(0).getId();
  }

  @Test
  @Transactional
  void handle_persistsNotesForTea() {
    replaceTeaTastingNotes.handle(command(
      note(10, "2026-07-21", "Excellent."),
      note(8, "2026-06-08", "Good.")));

    assertEquals(2, tastingNoteRepository.findByTeaIdNewestFirst(teaId).size());
  }

  @Test
  @Transactional
  void handle_calledAgain_replacesTheExistingNotes() {
    replaceTeaTastingNotes.handle(command(
      note(10, "2026-07-21", "First round A."),
      note(8, "2026-06-08", "First round B.")));

    replaceTeaTastingNotes.handle(command(note(9, "2026-08-01", "Second round only.")));

    var notes = tastingNoteRepository.findByTeaIdNewestFirst(teaId);
    assertEquals(1, notes.size());
    assertEquals("Second round only.", notes.get(0).getBody());
  }

  @Test
  @Transactional
  void handle_blankBodyNote_throws() {
    // proves the service builds (and validates) the TastingNote aggregate from the command's data
    var command = command(note(8, "2026-01-01", "   "));
    assertThrows(IllegalArgumentException.class, () -> replaceTeaTastingNotes.handle(command));
  }

  @Test
  @Transactional
  void handle_invalidTeaId_throws() {
    var command = new ReplaceTeaTastingNotesCommand(999_999L, userId, List.of(note(8, "2026-01-01", "x")));
    var ex = assertThrows(IllegalArgumentException.class, () -> replaceTeaTastingNotes.handle(command));
    assertTrue(ex.getMessage().toLowerCase().contains("tea"));
  }

  @Test
  @Transactional
  void handle_invalidUserId_throws() {
    var command = new ReplaceTeaTastingNotesCommand(teaId, 999_999L, List.of(note(8, "2026-01-01", "x")));
    var ex = assertThrows(IllegalArgumentException.class, () -> replaceTeaTastingNotes.handle(command));
    assertTrue(ex.getMessage().toLowerCase().contains("user"));
  }

  private ReplaceTeaTastingNotesCommand command(NoteData... notes) {
    return new ReplaceTeaTastingNotesCommand(teaId, userId, List.of(notes));
  }

  private static NoteData note(int halfStars, String date, String body) {
    return new NoteData(new Rating(halfStars), LocalDate.parse(date), body);
  }
}
