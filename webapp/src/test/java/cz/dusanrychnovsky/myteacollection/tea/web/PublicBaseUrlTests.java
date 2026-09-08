package cz.dusanrychnovsky.myteacollection.tea.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicBaseUrlTests {

  @Test
  void exposesNormalizedOriginAndComposesAbsoluteTeaUrl() {
    var baseUrl = new PublicBaseUrl("https://tea.example:8443/");

    assertEquals("https://tea.example:8443", baseUrl.value());
    assertEquals(
      "https://tea.example:8443/teas/mei-leaf-luminary-misfit-2022",
      baseUrl.teaUrl("mei-leaf-luminary-misfit-2022"));
  }

  @Test
  void acceptsHttpAndNormalizesSchemeAndTrailingSlash() {
    var baseUrl = new PublicBaseUrl("HTTP://localhost/");

    assertEquals("http://localhost/teas/vendor-title", baseUrl.teaUrl("vendor-title"));
  }

  @Test
  void rejectsMissingOrUnsupportedOriginParts() {
    assertInvalid(null);
    assertInvalid("localhost");
    assertInvalid("//tea.example");
    assertInvalid("ftp://tea.example");
    assertInvalid("https:/tea.example");
    assertInvalid("https://user@tea.example");
  }

  @Test
  void rejectsPathQueryAndFragment() {
    assertInvalid("https://tea.example/app");
    assertInvalid("https://tea.example/?query=value");
    assertInvalid("https://tea.example/#fragment");
  }

  @Test
  void rejectsInvalidResourceIdentifiers() {
    var baseUrl = new PublicBaseUrl("https://tea.example");

    assertThrows(IllegalArgumentException.class, () -> baseUrl.teaUrl("invalid"));
    assertThrows(IllegalArgumentException.class, () -> baseUrl.teaUrl("vendor-title/other"));
  }

  private static void assertInvalid(String value) {
    assertThrows(IllegalArgumentException.class, () -> new PublicBaseUrl(value));
  }
}