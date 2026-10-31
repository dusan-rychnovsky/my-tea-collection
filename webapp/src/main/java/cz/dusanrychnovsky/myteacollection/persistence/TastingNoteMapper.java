package cz.dusanrychnovsky.myteacollection.persistence;

import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;

/**
 * Maps tasting notes between domain and persistence, preserving intrinsic references.
 * The application resolves tea and taster entities before writing.
 */
public final class TastingNoteMapper {

  private TastingNoteMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static TastingNoteEntity toEntity(TastingNote note, TeaEntity tea, UserEntity taster) {
    if (tea == null || !note.getTeaId().equals(tea.getId())) {
      throw new IllegalArgumentException("Resolved tea must match the tasting note's tea.");
    }
    if (taster == null || !note.getTasterId().equals(taster.getId())) {
      throw new IllegalArgumentException("Resolved taster must match the tasting note's taster.");
    }

    return new TastingNoteEntity(
      tea,
      taster,
      note.getRating().halfStars(),
      note.getTastedOn(),
      note.getBody());
  }

  public static TastingNote toDomain(TastingNoteEntity entity) {
    return new TastingNote(
      entity.getTea().getId(),
      entity.getUser().getId(),
      new Rating(entity.getRatingHalfStars()),
      entity.getTastedOn(),
      entity.getBody());
  }
}
