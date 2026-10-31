package cz.dusanrychnovsky.myteacollection.persistence;

import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TastingNoteMapperTests {

  private static final LocalDate DATE = LocalDate.of(2026, 6, 8);
  private static final String BODY = "Great.\n\nReally.";

  @Test
  void toEntity_copiesFieldsAndAttachesTeaAndTaster() {
    var tea = tea(1);
    var taster = taster(2);
    var note = note();

    var entity = TastingNoteMapper.toEntity(note, tea, taster);

    assertNull(entity.getId());
    assertSame(tea, entity.getTea());
    assertSame(taster, entity.getUser());
    assertEquals(9, entity.getRatingHalfStars());
    assertEquals(DATE, entity.getTastedOn());
    assertEquals(BODY, entity.getBody());
  }

  @Test
  void toEntity_wrongTea_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> TastingNoteMapper.toEntity(note(), tea(99), taster(2)));
  }

  @Test
  void toEntity_wrongTaster_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> TastingNoteMapper.toEntity(note(), tea(1), taster(99)));
  }

  @Test
  void toEntity_nullTea_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> TastingNoteMapper.toEntity(note(), null, taster(2)));
  }

  @Test
  void toEntity_nullTaster_throws() {
    assertThrows(IllegalArgumentException.class,
      () -> TastingNoteMapper.toEntity(note(), tea(1), null));
  }

  @Test
  void toDomain_withoutPersistenceIdentity_restoresCompleteNote() {
    var entity = new TastingNoteEntity(tea(1), taster(2), 9, DATE, BODY);
    assertNull(entity.getId());

    var note = TastingNoteMapper.toDomain(entity);

    assertEquals(1L, note.getTeaId());
    assertEquals(2L, note.getTasterId());
    assertEquals(new Rating(9), note.getRating());
    assertEquals(DATE, note.getTastedOn());
    assertEquals(BODY, note.getBody());
  }

  @Test
  void toDomain_withPersistenceIdentity_restoresSameTastingData() {
    var entity = new TastingNoteEntity(tea(1), taster(2), 9, DATE, BODY);
    ReflectionTestUtils.setField(entity, "id", 99L);

    var note = TastingNoteMapper.toDomain(entity);

    assertEquals(1L, note.getTeaId());
    assertEquals(2L, note.getTasterId());
    assertEquals(new Rating(9), note.getRating());
    assertEquals(DATE, note.getTastedOn());
    assertEquals(BODY, note.getBody());
  }

  @Test
  void toDomain_blankBody_throws() {
    var entity = new TastingNoteEntity(tea(1), taster(2), 9, DATE, "   ");
    assertThrows(IllegalArgumentException.class, () -> TastingNoteMapper.toDomain(entity));
  }

  private static TastingNote note() {
    return new TastingNote(1L, 2L, new Rating(9), DATE, BODY);
  }

  private static TeaEntity tea(long id) {
    var tea = new TeaEntity();
    ReflectionTestUtils.setField(tea, "id", id);
    return tea;
  }

  private static UserEntity taster(long id) {
    var taster = new UserEntity();
    ReflectionTestUtils.setField(taster, "id", id);
    return taster;
  }
}
