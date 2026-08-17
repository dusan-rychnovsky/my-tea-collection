package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.domain.TastingNote;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TastingNoteMapperTests {

  @Test
  void toEntity_copiesFieldsAndAttachesTeaAndOwner() {
    var tea = new TeaEntity();
    var user = new UserEntity("owner@example.com", "pwd", "Dušan", "Rychnovský", null, null, null);
    var note = new TastingNote(new Rating(9), LocalDate.of(2026, 6, 8), "Great.\n\nReally.");

    var entity = TastingNoteMapper.toEntity(note, tea, user);

    assertSame(tea, entity.getTea());
    assertSame(user, entity.getUser());
    assertEquals(9, entity.getRatingHalfStars());
    assertEquals(LocalDate.of(2026, 6, 8), entity.getTastedOn());
    assertEquals("Great.\n\nReally.", entity.getBody());
  }
}
