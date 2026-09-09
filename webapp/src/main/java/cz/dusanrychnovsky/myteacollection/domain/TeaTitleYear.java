package cz.dusanrychnovsky.myteacollection.domain;

import java.util.Optional;

/**
 * Decides whether a tea's production year should be appended to a title-derived label.
 */
public final class TeaTitleYear {

  private TeaTitleYear() {
    throw new IllegalStateException("Utility class.");
  }

  public static Optional<Integer> suffixFor(String title, Optional<Season> season) {
    var titleYears = Season.yearsIn(title);
    var seasonYear = season.flatMap(Season::year);

    if (titleYears.size() == 1 && seasonYear.isPresent()) {
      var titleYear = titleYears.iterator().next();
      if (!titleYear.equals(seasonYear.get())) {
        throw new IllegalArgumentException(
          "Tea title year " + titleYear + " does not match season year " + seasonYear.get() + ".");
      }
    }

    return titleYears.isEmpty() ? seasonYear : Optional.empty();
  }
}