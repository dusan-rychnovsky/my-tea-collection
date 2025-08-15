package cz.dusanrychnovsky.myteacollection.util.upload;

import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.db.VendorEntity;
import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas.toEntity;
import static org.junit.jupiter.api.Assertions.*;

class UploadNewTeasTests {

  private static final Map<String, VendorEntity> VENDORS = Map.of(
    "Meetea", new VendorEntity(0L, "Meetea", "https://www.meetea.cz"),
    "Mei Leaf", new VendorEntity(1L, "Mei Leaf", "https://meileaf.com")
  );

  private static final Map<String, TeaTypeEntity> TEA_TYPES = Map.of(
    "Dark", new TeaTypeEntity(7L, "Dark"),
    "Sheng Puerh", new TeaTypeEntity(8L, "Sheng Puerh")
  );

  private static final Map<String, TagEntity> TAGS = Map.of(
    "meetea-2025-jan", new TagEntity(1L, "meetea-2025-jan", "Čajové předplatné Meetea, leden 2025"),
    "meetea-2024-dec", new TagEntity(2L, "meetea-2024-dec", "Čajové předplatné Meetea, prosinec 2024")
  );

  private static final TeaRecord TEA = new TeaRecord(
    "Luminary Misfit",
    "Lancang Gushu Sheng PuErh Spring 2022",
    "Ultra-fruity and fragrant PuErh made from ancient trees growing in Lancang. Toffee apples, pear compote, cardamom buns, canned pineapple and banana.",
    Set.of("Dark", "Sheng Puerh"),
    "Mei Leaf",
    "https://meileaf.com/tea/luminary-misfit/",
    "Lancang, Puer, Yunnan, China",
    "Da Ye Zhong",
    "April 2022",
    "1740-1970m",
    "95°C, 5g/100ml, 25+5s",
    true,
    Set.of("meetea-2025-jan", "meetea-2024-dec"))
    .setId(5);

  @Test
  void toEntity_translatesTeaToEntityRepresentation() {
    var result = toEntity(TEA, VENDORS, TEA_TYPES, TAGS);

    assertNull(result.getId());
    assertEquals(TEA.getTitle(), result.getTitle());
    assertEquals(TEA.getName(), result.getName());
    assertEquals(TEA.getDescription(), result.getDescription());
    assertEquals(TEA.getUrl(), result.getUrl());
    assertEquals(TEA.getOrigin(), result.getScope().getOrigin());
    assertEquals(TEA.getCultivar(), result.getScope().getCultivar());
    assertEquals(TEA.getSeason(), result.getScope().getSeason());
    assertEquals(TEA.getElevation(), result.getScope().getElevation());
    assertEquals(TEA.getBrewingInstructions(), result.getBrewingInstructions());
    assertEquals(TEA.isInStock(), result.isInStock());

    assertEquals(TEA.getVendor(), result.getVendor().getName());
    for (var type : TEA.getTypes()) {
      assertTrue(result.getTypes().stream().anyMatch(t -> t.getName().equals(type)));
    }
    for (var tag : TEA.getTags()) {
      assertTrue(result.getTags().stream().anyMatch(t -> t.getLabel().equals(tag)));
    }
  }

  @Test
  void toEntity_invalidVendor_throws() {
    var tea = withVendor(TEA, "Meileaf");
    assertThrows(IllegalArgumentException.class, () -> toEntity(tea, VENDORS, TEA_TYPES, TAGS));
  }

  @Test
  void toEntity_invalidType_throws() {
    var tea = withTypes(TEA, Set.of("Dark", "Blend"));
    assertThrows(IllegalArgumentException.class, () -> toEntity(tea, VENDORS, TEA_TYPES, TAGS));
  }

  @Test
  void toEntity_invalidTag_throws() {
    var tea = withTags(TEA, Set.of("meetea-2025-jan", "unknown-tag"));
    assertThrows(IllegalArgumentException.class, () -> toEntity(tea, VENDORS, TEA_TYPES, TAGS));
  }

  private static TeaRecord withTags(TeaRecord tea, Set<String> tags) {
    return new TeaRecord(
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getTypes(),
      tea.getVendor(),
      tea.getUrl(),
      tea.getOrigin(),
      tea.getCultivar(),
      tea.getSeason(),
      tea.getElevation(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tags
    )
      .setId(tea.getId());
  }

  private static TeaRecord withTypes(TeaRecord tea, Set<String> types) {
    return new TeaRecord(
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      types,
      tea.getVendor(),
      tea.getUrl(),
      tea.getOrigin(),
      tea.getCultivar(),
      tea.getSeason(),
      tea.getElevation(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tea.getTags()
    )
      .setId(tea.getId());
  }

  private static TeaRecord withVendor(TeaRecord tea, String vendor) {
    return new TeaRecord(
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getTypes(),
      vendor,
      tea.getUrl(),
      tea.getOrigin(),
      tea.getCultivar(),
      tea.getSeason(),
      tea.getElevation(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tea.getTags()
    )
      .setId(tea.getId());
  }
}
