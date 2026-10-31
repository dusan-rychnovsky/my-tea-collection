package cz.dusanrychnovsky.myteacollection.tastingnotes.application;

import cz.dusanrychnovsky.myteacollection.domain.LatestTasterRatings;
import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteMapper;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for the "replace a tea's tasting notes" use case, used by the tasting-note
 * ingest. Builds each {@link TastingNote} aggregate from the command (enforcing its invariants),
 * validates that the referenced tea and taster exist, then atomically replaces the tea's notes —
 * deleting the existing ones, saving the new ones and updating the tea's materialized average
 * rating in a single transaction, so a failure leaves the tea's previous state intact.
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
      .map(data -> new TastingNote(
        command.teaId(), command.userId(), data.rating(), data.tastedOn(), data.body()))
      .toList();

    var taster = userRepository.findById(command.userId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + command.userId()));

    var tea = teaRepository.findById(command.teaId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid tea ID: " + command.teaId()));

    var entities = notes.stream()
      .map(note -> TastingNoteMapper.toEntity(note, tea, taster))
      .toList();

    tastingNoteRepository.deleteByTeaId(tea.getId());
    tastingNoteRepository.saveAllAndFlush(entities);

    // Use the detail page's history order so arbitrary same-date choices agree.
    var history = tastingNoteRepository.findByTeaIdNewestFirst(tea.getId());
    var latestRatings = LatestTasterRatings.of(history.stream()
      .map(TastingNoteMapper::toDomain)
      .toList());
    var average = latestRatings.averageHalfStars();
    var averageRatingHalfStars = average.isPresent() ? average.getAsDouble() : null;
    tea.setAverageRatingHalfStars(averageRatingHalfStars);
    teaRepository.saveAndFlush(tea);
  }
}
