package cz.dusanrychnovsky.myteacollection.util.upload;

import cz.dusanrychnovsky.myteacollection.db.TeaType;
import cz.dusanrychnovsky.myteacollection.db.Vendor;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas.toEntity;
import static org.junit.jupiter.api.Assertions.*;

public class UploadNewTeasTests {

  private final Map<String, Vendor> VENDORS = Map.of(
    "Meetea", new Vendor(0L, "Meetea", "https://www.meetea.cz"),
    "Mei Leaf", new Vendor(1L, "Mei Leaf", "https://meileaf.com")
  );

  private final Map<Long, TeaType> TEA_TYPES = Map.of(
    7L, new TeaType(7L, "Dark"),
    8L, new TeaType(8L, "Sheng Puerh")
  );

  private final Tea TEA = new Tea(
    "Luminary Misfit",
    "Lancang Gushu Sheng PuErh Spring 2022",
    "Ultra-fruity and fragrant PuErh made from ancient trees growing in Lancang. Toffee apples, pear compote, cardamom buns, canned pineapple and banana.",
    Set.of(7L, 8L),
    "Mei Leaf",
    "https://meileaf.com/tea/luminary-misfit/",
    "Lancang, Puer, Yunnan, China",
    "Da Ye Zhong",
    "April 2022",
    "1740-1970m",
    "95°C, 5g/100ml, 25+5s",
    true)
    .setId(5);

  @Test
  public void toEntity_translatesTeaToEntityRepresentation() {
    var result = toEntity(TEA, VENDORS, TEA_TYPES);

    assertEquals(TEA.getId(), result.getId());
    assertEquals(TEA.getTitle(), result.getTitle());
    assertEquals(TEA.getName(), result.getName());
    assertEquals(TEA.getDescription(), result.getDescription());
    assertEquals(TEA.getUrl(), result.getUrl());
    assertEquals(TEA.getOrigin(), result.getOrigin());
    assertEquals(TEA.getCultivar(), result.getCultivar());
    assertEquals(TEA.getSeason(), result.getSeason());
    assertEquals(TEA.getElevation(), result.getElevation());
    assertEquals(TEA.getBrewingInstructions(), result.getBrewingInstructions());
    assertEquals(TEA.isInStock(), result.isInStock());

    assertEquals(TEA.getVendor(), result.getVendor().getName());
    for (var typeId : TEA.getTypeIds()) {
      assertTrue(result.getTypes().stream().anyMatch(type -> type.getId() == typeId));
    }
  }

  @Test
  public void toEntity_invalidVendor_throws() {
    var tea = withVendor(TEA, "Meileaf");
    assertThrows(IllegalArgumentException.class, () -> toEntity(tea, VENDORS, TEA_TYPES));
  }

  private static Tea withVendor(Tea tea, String vendor) {
    return new Tea(
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getTypeIds(),
      vendor,
      tea.getUrl(),
      tea.getOrigin(),
      tea.getCultivar(),
      tea.getSeason(),
      tea.getElevation(),
      tea.getBrewingInstructions(),
      tea.isInStock()
    )
      .setId(tea.getId());
  }
}
