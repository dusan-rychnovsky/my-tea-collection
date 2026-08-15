package cz.dusanrychnovsky.myteacollection.query;

import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.domain.Price;

import java.net.MalformedURLException;
import java.net.URL;
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
    var imageIdsByIndex = tea.getImages().stream()
      .sorted(comparingInt(TeaImageEntity::getIndex))
      .map(TeaImageEntity::getId)
      .toList();
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
      priceLabel(tea.getPrice()),
      tea.getBrewingInstructions(),
      imageIdsByIndex.stream().findFirst().orElse(null),
      imageIdsByIndex.stream().skip(1).toList()
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

  private static String priceLabel(Float amountPerGram) {
    return amountPerGram == null ? "N/A" : new Price(amountPerGram).label();
  }

  private static String urlDomain(TeaEntity tea) {
    try {
      return new URL(tea.getUrl()).getHost();
    }
    catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Malformed tea URL: " + tea.getUrl(), ex);
    }
  }
}
