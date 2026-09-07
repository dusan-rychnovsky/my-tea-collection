package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaSlugTests {

  @Test
  void from_normalizesVendorAndTitle() {
    assertEquals(
      new TeaSlug("shancha-tea-cafe-shans-choice"),
      TeaSlug.from(tea("Café — Shan's Choice", "N/A"), "Shānchá Tea"));
    assertEquals(
      new TeaSlug("shancha-tea-shans-choice"),
      TeaSlug.from(tea("Shan\u2019s Choice", null), "Shānchá Tea"));
    assertEquals(
      new TeaSlug("vendor-tea-name"),
      TeaSlug.from(tea("--- Name ---", "N/A"), "Vendor 😀 / Tea"));
  }

  @Test
  void from_titleContainsYear_doesNotAppendSeasonYear() {
    assertEquals(
      new TeaSlug("mei-leaf-luminary-misfit-2022"),
      TeaSlug.from(tea("Luminary Misfit 2022", "Q3 2022"), "Mei Leaf"));
  }

  @Test
  void from_titleHasNoYear_appendsSingleDistinctSeasonYear() {
    assertEquals(
      new TeaSlug("mei-leaf-luminary-misfit-1900"),
      TeaSlug.from(tea("Luminary Misfit", "Spring 1900"), "Mei Leaf"));
    assertEquals(
      new TeaSlug("mei-leaf-luminary-misfit-2099"),
      TeaSlug.from(tea("Luminary Misfit", "2099/2099"), "Mei Leaf"));
  }

  @Test
  void from_nonYearValues_doesNotAppendSuffix() {
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "1899"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "2100"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "8582"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "1990s"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "2020s"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "Spring2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea"), TeaSlug.from(tea("Tea", "2022abc"), "Vendor"));
  }

  @Test
  void from_nonYearValuesInTitle_appendsSeasonYear() {
    assertEquals(new TeaSlug("vendor-tea-1899-2022"), TeaSlug.from(tea("Tea 1899", "2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea-2100-2022"), TeaSlug.from(tea("Tea 2100", "2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea-8582-2022"), TeaSlug.from(tea("Tea 8582", "2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea-1990s-2022"), TeaSlug.from(tea("Tea 1990s", "2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea-2020s-2022"), TeaSlug.from(tea("Tea 2020s", "2022"), "Vendor"));
    assertEquals(new TeaSlug("vendor-spring2022-tea-2023"), TeaSlug.from(tea("Spring2022 Tea", "2023"), "Vendor"));
    assertEquals(new TeaSlug("vendor-tea-2022abc-2023"), TeaSlug.from(tea("Tea 2022abc", "2023"), "Vendor"));
  }

  @Test
  void from_multipleDistinctSeasonYears_doesNotAppendSuffix() {
    assertEquals(
      new TeaSlug("vendor-tea"),
      TeaSlug.from(tea("Tea", "2020-2022"), "Vendor"));
  }

  @Test
  void from_differentSingleTitleAndSeasonYears_throwsWithBothValues() {
    var exception = assertThrows(
      IllegalArgumentException.class,
      () -> TeaSlug.from(tea("Tea 2021", "Spring 2022"), "Vendor"));

    assertTrue(exception.getMessage().contains("title year 2021"));
    assertTrue(exception.getMessage().contains("season year 2022"));
  }

  @Test
  void from_emptyNormalizedComponent_throws() {
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(null, "Vendor"));
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(tea("Tea", "2022"), null));
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(tea("Tea", "2022"), "---"));
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(tea("😀", "2022"), "Vendor"));
  }

  @Test
  void construct_invalidOrReservedSlug_throws() {
    assertThrows(IllegalArgumentException.class, () -> new TeaSlug(null));
    assertThrows(IllegalArgumentException.class, () -> new TeaSlug("123"));
    assertThrows(IllegalArgumentException.class, () -> new TeaSlug("2024-2025"));
    assertThrows(IllegalArgumentException.class, () -> new TeaSlug("add"));
    assertThrows(IllegalArgumentException.class, () -> new TeaSlug("Vendor-tea"));
  }

  @Test
  void from_allNumericComponents_throws() {
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(tea("2025", null), "2024"));
  }

  @Test
  void from_individuallyNumericComponent_isAllowed() {
    assertEquals(new TeaSlug("2024-tea"), TeaSlug.from(tea("Tea", "N/A"), "2024"));
  }

  @Test
  void from_lengthBoundary_accepts255AndRejects256Characters() {
    var title253 = "t".repeat(253);
    var title254 = "t".repeat(254);

    assertEquals("v-" + title253, TeaSlug.from(tea(title253, null), "v").value());
    assertThrows(IllegalArgumentException.class, () -> TeaSlug.from(tea(title254, null), "v"));
  }

  private static Tea tea(String title, String season) {
    return new Tea(
      title,
      "",
      "Description",
      "https://example.com/tea",
      new TeaScope(season, "Cultivar", "Origin", "1000m"),
      null,
      "95°C",
      true,
      1L,
      Set.of(1L),
      Set.of(),
      List.of(new byte[]{1}));
  }
}