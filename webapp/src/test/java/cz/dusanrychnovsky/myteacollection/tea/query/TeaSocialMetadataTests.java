package cz.dusanrychnovsky.myteacollection.tea.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeaSocialMetadataTests {

  @Test
  void from_exactSeasonYear_appendsVendorAndYearToTitle() {
    var metadata = TeaSocialMetadata.from(tea("Luminary Misfit", "Technical name", "April 2022"));

    assertEquals("Luminary Misfit (Mei Leaf, 2022)", metadata.title());
  }

  @Test
  void from_yearAlreadyInTitle_appendsOnlyVendor() {
    var metadata = TeaSocialMetadata.from(tea("Shou Mei 2017", "Technical name", "2017"));

    assertEquals("Shou Mei 2017 (Mei Leaf)", metadata.title());
  }

  @Test
  void from_absentOrApproximateSeason_appendsOnlyVendor() {
    assertEquals(
      "Jade Star 8 (Mei Leaf)",
      TeaSocialMetadata.from(tea("Jade Star 8", "Technical name", null)).title());
    assertEquals(
      "Aged Tea (Mei Leaf)",
      TeaSocialMetadata.from(tea("Aged Tea", "Technical name", "Early 2000s")).title());
  }

  @Test
  void from_presentTechnicalName_prependsNameToDescription() {
    var metadata = TeaSocialMetadata.from(tea("Tea", "Technical name", "2022"));

    assertEquals("Technical name. Tea description.", metadata.description());
  }

  @Test
  void from_blankOrNullTechnicalName_usesDescriptionOnly() {
    assertEquals(
      "Tea description.",
      TeaSocialMetadata.from(tea("Tea", "", "2022")).description());
    assertEquals(
      "Tea description.",
      TeaSocialMetadata.from(tea("Tea", "  ", "2022")).description());
    assertEquals(
      "Tea description.",
      TeaSocialMetadata.from(tea("Tea", null, "2022")).description());
  }

  @Test
  void from_invalidPersistedSeason_failsFast() {
    assertThrows(
      IllegalArgumentException.class,
      () -> TeaSocialMetadata.from(tea("Tea", "Technical name", "2021 and 2022")));
  }

  private static TeaDetail tea(String title, String name, String season) {
    return new TeaDetail(
      1L,
      "mei-leaf-tea",
      title,
      name,
      "Tea description.",
      "Mei Leaf",
      List.of("White Tea"),
      List.of(),
      "https://example.com/tea",
      "example.com",
      new TeaScope(season, "Cultivar", "Origin", "1000m"),
      "N/A",
      "95C",
      1L,
      List.of());
  }
}