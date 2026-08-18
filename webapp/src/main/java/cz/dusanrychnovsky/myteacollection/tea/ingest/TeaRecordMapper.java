package cz.dusanrychnovsky.myteacollection.tea.ingest;

import cz.dusanrychnovsky.myteacollection.tea.application.AddTeaCommand;
import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.VendorEntity;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;

import java.util.List;
import java.util.Map;

import static cz.dusanrychnovsky.myteacollection.util.MapUtils.getOrThrow;
import static cz.dusanrychnovsky.myteacollection.util.MapUtils.mapAll;
import static java.util.stream.Collectors.toSet;

/**
 * Ingest anti-corruption layer: translates an external {@link TeaRecord} (which names its
 * vendor / types / tags) into an {@link AddTeaCommand} (which references them by id), resolving
 * those names against the prefetched reference-data maps and parsing the price. Throws
 * {@link IllegalArgumentException} on an unknown vendor / type / tag name.
 */
public final class TeaRecordMapper {

  private TeaRecordMapper() {
    throw new IllegalStateException("Utility class.");
  }

  public static AddTeaCommand toCommand(
    long userId,
    TeaRecord tea,
    List<byte[]> images,
    Map<String, VendorEntity> vendors,
    Map<String, TeaTypeEntity> teaTypes,
    Map<String, TagEntity> tags) {

    var vendorId = getOrThrow(vendors, tea.getVendor()).getId();
    var typeIds = mapAll(teaTypes, tea.getTypes()).stream().map(TeaTypeEntity::getId).collect(toSet());
    var tagIds = mapAll(tags, tea.getTags()).stream().map(TagEntity::getId).collect(toSet());
    var price = Price.parse(tea.getPrice()).orElse(null);

    return new AddTeaCommand(
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getUrl(),
      new TeaScope(tea.getSeason(), tea.getCultivar(), tea.getOrigin(), tea.getElevation()),
      price,
      tea.getBrewingInstructions(),
      tea.isInStock(),
      userId,
      vendorId,
      typeIds,
      tagIds,
      images);
  }
}
