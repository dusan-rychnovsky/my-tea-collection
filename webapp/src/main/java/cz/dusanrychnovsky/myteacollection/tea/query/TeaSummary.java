package cz.dusanrychnovsky.myteacollection.tea.query;

import java.util.List;
import java.util.Locale;

public record TeaSummary(
  Long id,
  String slug,
  String title,
  String name,
  String vendorName,
  String typeNames,
  String description,
  Double averageRatingHalfStars,
  Long mainImageId,
  List<TeaTag> tags
) {

  public String averageRatingLabel() {
    return averageRatingHalfStars == null
      ? null
      : String.format(Locale.ENGLISH, "%.1f", averageRatingHalfStars / 2.0);
  }
}
