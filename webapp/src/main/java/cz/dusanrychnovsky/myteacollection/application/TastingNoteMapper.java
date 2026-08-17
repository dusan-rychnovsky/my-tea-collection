package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;

/**
 * Maps a write-side domain {@link TastingNote} plus its already-resolved tea and owner entities to
 * a persistence {@link TastingNoteEntity}. Pure and stateless; reference resolution lives in
 * {@link ReplaceTeaTastingNotes}, not here.
 */
public final class TastingNoteMapper {

  private TastingNoteMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static TastingNoteEntity toEntity(TastingNote note, TeaEntity tea, UserEntity user) {
    return new TastingNoteEntity(
      tea,
      user,
      note.getRating().halfStars(),
      note.getTastedOn(),
      note.getBody());
  }
}
