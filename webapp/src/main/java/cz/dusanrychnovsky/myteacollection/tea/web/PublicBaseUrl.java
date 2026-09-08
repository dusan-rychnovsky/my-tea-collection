package cz.dusanrychnovsky.myteacollection.tea.web;

import cz.dusanrychnovsky.myteacollection.domain.TeaSlug;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Component
public final class PublicBaseUrl {

  private final String value;

  public PublicBaseUrl(@Value("${app.public-base-url}") String value) {
    var uri = parse(value);
    var scheme = uri.getScheme();
    var path = uri.getRawPath();
    if (scheme == null
      || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
      || uri.getRawAuthority() == null
      || uri.getHost() == null
      || uri.getRawUserInfo() != null
      || !(path == null || path.isEmpty() || path.equals("/"))
      || uri.getRawQuery() != null
      || uri.getRawFragment() != null) {
      throw new IllegalArgumentException("Public base URL must be an HTTP(S) origin: " + value);
    }
    this.value = scheme.toLowerCase(Locale.ROOT) + "://" + uri.getRawAuthority();
  }

  public String teaUrl(String slug) {
    return value + "/teas/" + new TeaSlug(slug).value();
  }

  public String value() {
    return value;
  }

  private static URI parse(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Public base URL must not be null.");
    }
    try {
      return new URI(value);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid public base URL: " + value, ex);
    }
  }
}