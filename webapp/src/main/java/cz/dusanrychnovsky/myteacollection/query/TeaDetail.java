package cz.dusanrychnovsky.myteacollection.query;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;

import java.net.MalformedURLException;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;

public record TeaDetail(
  Long id,
  String title,
  String name,
  String description,
  String vendorName,
  List<String> typeNames,
  List<String> tagLabels,
  String url,
  String urlDomain,
  TeaScope scope,
  String priceLabel,
  String brewingInstructions,
  Long mainImageId,
  List<Long> additionalImageIds
) {

  public static TeaDetail from(TeaEntity tea) {
    return new TeaDetail(
      tea.getId(),
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getVendor().getName(),
      tea.getTypes().stream()
        .sorted(comparingLong(TeaTypeEntity::getId))
        .map(TeaTypeEntity::getName)
        .toList(),
      tea.getTags().stream()
        .sorted(comparing(TagEntity::getId))
        .map(TagEntity::getLabel)
        .toList(),
      tea.getUrl(),
      urlDomain(tea),
      scope(tea.getScope()),
      tea.printPrice(),
      tea.getBrewingInstructions(),
      tea.getMainImage().map(TeaImageEntity::getId).orElse(null),
      tea.getAdditionalImages().stream()
        .sorted(comparingInt(TeaImageEntity::getIndex))
        .map(TeaImageEntity::getId)
        .toList()
    );
  }

  private static TeaScope scope(TeaScopeEntity scope) {
    return new TeaScope(
      scope.getSeason(),
      scope.getCultivar(),
      scope.getOrigin(),
      scope.getElevation()
    );
  }

  private static String urlDomain(TeaEntity tea) {
    try {
      return tea.getUrlDomain();
    }
    catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Malformed tea URL: " + tea.getUrl(), ex);
    }
  }
}
