package cz.dusanrychnovsky.myteacollection.tea.query;

import cz.dusanrychnovsky.myteacollection.domain.Season;
import cz.dusanrychnovsky.myteacollection.domain.TeaTitleYear;

import java.util.Optional;

public record TeaSocialMetadata(String title, String description) {

  public static TeaSocialMetadata from(TeaDetail tea) {
    var season = Optional.ofNullable(tea.scope().season()).map(Season::new);
    var suffixYear = TeaTitleYear.suffixFor(tea.title(), season);
    var titleContext = tea.vendorName() + suffixYear.map(year -> ", " + year).orElse("");
    var description = tea.name() == null || tea.name().isBlank()
      ? tea.description()
      : tea.name() + ". " + tea.description();

    return new TeaSocialMetadata(tea.title() + " (" + titleContext + ")", description);
  }
}