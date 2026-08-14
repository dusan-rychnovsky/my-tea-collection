package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageDataEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.db.VendorEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.domain.Tea;

import java.util.Set;

/**
 * Maps a write-side domain {@link Tea} plus its already-resolved reference entities to a
 * persistence {@link TeaEntity}. Pure and stateless reference resolution and validation
 * live in {@link AddTea}, not here.
 */
public final class TeaMapper {

  private TeaMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static TeaEntity toEntity(
    Tea tea,
    UserEntity user,
    VendorEntity vendor,
    Set<TeaTypeEntity> types,
    Set<TagEntity> tags) {

    var scope = tea.getScope();
    var entity = new TeaEntity(
      user,
      vendor,
      types,
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getUrl(),
      new TeaScopeEntity(scope.season(), scope.cultivar(), scope.origin(), scope.elevation()),
      tea.getPrice() == null ? null : tea.getPrice().amountPerGram(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tags);

    var index = 0;
    for (var imageBytes : tea.getImages()) {
      index++;
      entity.addImage(new TeaImageEntity(entity, index, new TeaImageDataEntity(imageBytes)));
    }
    return entity;
  }
}
