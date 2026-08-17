package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the "replace a tea's tasting notes" use case, used by the tasting-note
 * ingest. Builds each {@link TastingNote} aggregate from the command (enforcing its invariants),
 * validates that the referenced tea and owner exist, then atomically replaces the tea's notes —
 * deleting the existing ones and saving the new ones in a single transaction,
 * so a failure leaves the tea's previous notes intact.
 */
@Service
public class ReplaceTeaTastingNotes {

  private final TastingNoteRepository tastingNoteRepository;
  private final TeaRepository teaRepository;
  private final UserRepository userRepository;

  @Autowired
  public ReplaceTeaTastingNotes(
    TastingNoteRepository tastingNoteRepository,
    TeaRepository teaRepository,
    UserRepository userRepository) {

    this.tastingNoteRepository = tastingNoteRepository;
    this.teaRepository = teaRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public void handle(ReplaceTeaTastingNotesCommand command) {
    var notes = command.notes().stream()
      .map(data -> new TastingNote(data.rating(), data.tastedOn(), data.body()))
      .toList();

    var user = userRepository.findById(command.userId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + command.userId()));

    var tea = teaRepository.findById(command.teaId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid tea ID: " + command.teaId()));

    var entities = notes.stream()
      .map(note -> TastingNoteMapper.toEntity(note, tea, user))
      .toList();

    tastingNoteRepository.deleteByTeaId(tea.getId());
    tastingNoteRepository.saveAll(entities);
  }
}
