package cz.dusanrychnovsky.myteacollection.tea.query;

import java.util.List;

public record TeaSummary(
  Long id,
  String slug,
  String title,
  String name,
  String vendorName,
  String typeNames,
  String description,
  Long mainImageId,
  List<TeaTag> tags
) {
}
