package cz.dusanrychnovsky.myteacollection.query;

import java.util.List;

public record TeaSummary(
  Long id,
  String title,
  String name,
  String vendorName,
  String typeLabels,
  String description,
  Long mainImageId,
  List<TeaTag> tags
) {
}
