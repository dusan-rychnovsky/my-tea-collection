package cz.dusanrychnovsky.myteacollection.domain;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record TeaSlug(String value) {

  private static final int MAX_LENGTH = 255;
  private static final Pattern VALID_SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)+");
  private static final Pattern ALL_NUMERIC = Pattern.compile("[0-9]+(?:-[0-9]+)+");
  private static final Pattern YEAR = Pattern.compile(
    "(?<![A-Za-z0-9])((?:19|20)[0-9]{2})(?![A-Za-z0-9])");

  public TeaSlug {
    if (value == null || !VALID_SLUG.matcher(value).matches()) {
      throw new IllegalArgumentException("Invalid tea slug: " + value);
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException(
        "Tea slug must not exceed " + MAX_LENGTH + " characters: " + value.length());
    }
    if (ALL_NUMERIC.matcher(value).matches()) {
      throw new IllegalArgumentException("Tea slug must not be all-numeric: " + value);
    }
    if (value.equals("add")) {
      throw new IllegalArgumentException("Tea slug is reserved: " + value);
    }
  }

  public static TeaSlug from(Tea tea, String vendorName) {
    if (tea == null) {
      throw new IllegalArgumentException("Tea must not be null.");
    }

    var title = tea.getTitle();
    var season = tea.getScope().season();
    var normalizedVendor = normalize(vendorName, "vendor name");
    var normalizedTitle = normalize(title, "title");
    var titleYears = yearsIn(title);
    var seasonYears = yearsIn(season);

    if (titleYears.size() == 1 && seasonYears.size() == 1) {
      var titleYear = titleYears.iterator().next();
      var seasonYear = seasonYears.iterator().next();
      if (!titleYear.equals(seasonYear)) {
        throw new IllegalArgumentException(
          "Tea title year " + titleYear + " does not match season year " + seasonYear + ".");
      }
    }

    var value = normalizedVendor + "-" + normalizedTitle;
    if (titleYears.isEmpty() && seasonYears.size() == 1) {
      value += "-" + seasonYears.iterator().next();
    }
    return new TeaSlug(value);
  }

  private static String normalize(String component, String componentName) {
    if (component == null) {
      throw new IllegalArgumentException("Tea " + componentName + " must not be null.");
    }

    var decomposed = Normalizer.normalize(component, Normalizer.Form.NFKD)
      .toLowerCase(Locale.ROOT);
    var normalized = new StringBuilder();
    var separatorPending = false;
    for (var index = 0; index < decomposed.length();) {
      var codePoint = decomposed.codePointAt(index);
      index += Character.charCount(codePoint);

      var type = Character.getType(codePoint);
      if (type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || type == Character.ENCLOSING_MARK
        || codePoint == '\''
        || codePoint == 0x2019) {
        continue;
      }

      if (codePoint >= 'a' && codePoint <= 'z' || codePoint >= '0' && codePoint <= '9') {
        if (separatorPending) {
          normalized.append('-');
        }
        normalized.appendCodePoint(codePoint);
        separatorPending = false;
      }
      else if (!normalized.isEmpty()) {
        separatorPending = true;
      }
    }

    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("Tea " + componentName + " must contain a letter or digit.");
    }
    return normalized.toString();
  }

  private static Set<String> yearsIn(String text) {
    var years = new LinkedHashSet<String>();
    if (text == null) {
      return years;
    }
    var matcher = YEAR.matcher(text);
    while (matcher.find()) {
      years.add(matcher.group(1));
    }
    return years;
  }
}