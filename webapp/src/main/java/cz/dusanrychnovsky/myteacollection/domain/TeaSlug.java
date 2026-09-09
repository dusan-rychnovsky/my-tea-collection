package cz.dusanrychnovsky.myteacollection.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public record TeaSlug(String value) {

  private static final int MAX_LENGTH = 255;
  private static final Pattern VALID_SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)+");
  private static final Pattern ALL_NUMERIC = Pattern.compile("[0-9]+(?:-[0-9]+)+");

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
    var normalizedVendor = normalize(vendorName, "vendor name");
    var normalizedTitle = normalize(title, "title");

    var value = normalizedVendor + "-" + normalizedTitle;
    var suffixYear = TeaTitleYear.suffixFor(title, tea.getScope().season());
    if (suffixYear.isPresent()) {
      value += "-" + suffixYear.get();
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

}