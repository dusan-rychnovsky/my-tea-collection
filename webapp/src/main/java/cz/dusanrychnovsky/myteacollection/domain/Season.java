package cz.dusanrychnovsky.myteacollection.domain;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A meaningful description of a tea's production season, optionally containing one exact year.
 * Absence of a season is represented outside this type.
 */
public record Season(String value) {

  private static final Pattern YEAR = Pattern.compile(
    "(?<![A-Za-z0-9])((?:19|20)[0-9]{2})(?![A-Za-z0-9])");

  public Season {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("Season must not be blank.");
    }

    var years = yearsIn(value);
    if (years.size() > 1) {
      throw new IllegalArgumentException(
        "Season must contain at most one exact year: " + value + " (found " + years + ")");
    }
  }

  public Optional<Integer> year() {
    return yearsIn(value).stream().findFirst();
  }

  static Set<Integer> yearsIn(String text) {
    var years = new LinkedHashSet<Integer>();
    if (text == null) {
      return years;
    }

    var matcher = YEAR.matcher(text);
    while (matcher.find()) {
      years.add(Integer.parseInt(matcher.group(1)));
    }
    return years;
  }
}