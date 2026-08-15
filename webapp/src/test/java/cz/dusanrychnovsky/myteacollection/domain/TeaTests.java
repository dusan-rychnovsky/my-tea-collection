package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaTests {

  private static final TeaScope SCOPE = new TeaScope("Spring 2024", "Da Ye Zhong", "Yunnan", "1500m");
  private static final List<byte[]> IMAGES = List.of(new byte[]{1, 2});
  private static final Long VENDOR_ID = 2L;
  private static final Set<Long> TYPE_IDS = Set.of(25L);
  private static final Set<Long> TAG_IDS = Set.of();

  private static Tea tea(
    String title, String name, String description, String url, TeaScope scope, List<byte[]> images) {
    return new Tea(title, name, description, url, scope, new Price(5f), "95°C", true,
      VENDOR_ID, TYPE_IDS, TAG_IDS, images);
  }

  private static Tea teaWithRefs(Long vendorId, Set<Long> typeIds, Set<Long> tagIds) {
    return new Tea("Title", "name", "desc", "https://example.com/y", SCOPE,
      new Price(5f), "95°C", true, vendorId, typeIds, tagIds, IMAGES);
  }

  private static Tea validTea() {
    return tea(
      "Luminary Misfit", "Some name", "A fruity puerh.",
      "https://meileaf.com/tea/luminary-misfit", SCOPE, IMAGES);
  }

  @Test
  void constructs_whenAllInvariantsHold() {
    assertDoesNotThrow(TeaTests::validTea);
  }

  @Test
  void allowsBlankName() {
    assertDoesNotThrow(() -> tea("Title", "", "desc", "https://example.com/y", SCOPE, IMAGES));
  }

  @Test
  void rejectsBlankTitle() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      tea("  ", "name", "desc", "https://example.com/y", SCOPE, IMAGES));
    assertTrue(ex.getMessage().contains("title"));
  }

  @Test
  void rejectsNullTitle() {
    assertThrows(IllegalArgumentException.class, () ->
      tea(null, "name", "desc", "https://example.com/y", SCOPE, IMAGES));
  }

  @Test
  void rejectsBlankDescription() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "", "https://example.com/y", SCOPE, IMAGES));
    assertTrue(ex.getMessage().contains("description"));
  }

  @Test
  void rejectsBlankUrl() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "desc", "  ", SCOPE, IMAGES));
    assertTrue(ex.getMessage().contains("url"));
  }

  @Test
  void rejectsMalformedUrl() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "desc", "not-a-url", SCOPE, IMAGES));
    assertTrue(ex.getMessage().contains("url"));
  }

  @Test
  void rejectsNullScope() {
    assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "desc", "https://example.com/y", null, IMAGES));
  }

  @Test
  void rejectsEmptyImages() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "desc", "https://example.com/y", SCOPE, List.of()));
    assertTrue(ex.getMessage().contains("image"));
  }

  @Test
  void rejectsNullImages() {
    assertThrows(IllegalArgumentException.class, () ->
      tea("Title", "name", "desc", "https://example.com/y", SCOPE, null));
  }

  @Test
  void rejectsNullVendor() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      teaWithRefs(null, TYPE_IDS, TAG_IDS));
    assertTrue(ex.getMessage().contains("vendor"));
  }

  @Test
  void rejectsEmptyTypes() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      teaWithRefs(VENDOR_ID, Set.of(), TAG_IDS));
    assertTrue(ex.getMessage().contains("type"));
  }

  @Test
  void rejectsNullTypes() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      teaWithRefs(VENDOR_ID, null, TAG_IDS));
    assertTrue(ex.getMessage().contains("type"));
  }

  @Test
  void rejectsNullTags() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      teaWithRefs(VENDOR_ID, TYPE_IDS, null));
    assertTrue(ex.getMessage().contains("tags"));
  }

  @Test
  void allowsEmptyTags() {
    assertDoesNotThrow(() -> teaWithRefs(VENDOR_ID, TYPE_IDS, Set.of()));
  }
}
