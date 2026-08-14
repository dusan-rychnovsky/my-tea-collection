package cz.dusanrychnovsky.myteacollection.ingest;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.db.VendorEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.domain.Price;

import java.util.Map;

import static cz.dusanrychnovsky.myteacollection.util.MapUtils.getOrThrow;
import static cz.dusanrychnovsky.myteacollection.util.MapUtils.mapAll;

public class TeaRecordMapper {

  private TeaRecordMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static TeaEntity toEntity(
    UserEntity userEntity,
    TeaRecord tea,
    Map<String, VendorEntity> vendors,
    Map<String, TeaTypeEntity> teaTypes,
    Map<String, TagEntity> tags) {

    var vendorEntity = getOrThrow(vendors, tea.getVendor());
    var typeEntities = mapAll(teaTypes, tea.getTypes());
    var tagEntities = mapAll(tags, tea.getTags());

    var price = Price.parse(tea.getPrice()).map(Price::amountPerGram).orElse(null);

    return new TeaEntity(
      userEntity,
      vendorEntity,
      typeEntities,
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getUrl(),
      new TeaScopeEntity(
        tea.getSeason(),
        tea.getCultivar(),
        tea.getOrigin(),
        tea.getElevation()
      ),
      price,
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tagEntities
    );
  }
}
