package cz.dusanrychnovsky.myteacollection.ingest;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.db.VendorEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeaRecordMapperTests {

  private static final long USER_ID = 42L;

  private static final UserEntity USER = new UserEntity(
    "dusan.rychnovsky@gmail.com",
    "pwd",
    "Dušan",
    "Rychnovský",
    null,
    "Prague, Czech republic",
    null);

  private static final Map<String, VendorEntity> VENDORS = Map.of(
    "Meetea", new VendorEntity(0L, "Meetea", "https://www.meetea.cz"),
    "Mei Leaf", new VendorEntity(1L, "Mei Leaf", "https://meileaf.com")
  );

  private static final Map<String, TeaTypeEntity> TEA_TYPES = Map.of(
    "Dark", new TeaTypeEntity(7L, "Dark"),
    "Sheng Puerh", new TeaTypeEntity(8L, "Sheng Puerh")
  );

  private static final Map<String, TagEntity> TAGS = Map.of(
    "meetea-2025-jan", new TagEntity(1L, USER, "meetea-2025-jan", "Čajové předplatné Meetea, leden 2025"),
    "meetea-2024-dec", new TagEntity(2L, USER, "meetea-2024-dec", "Čajové předplatné Meetea, prosinec 2024")
  );

  private static final List<byte[]> IMAGES = List.of(new byte[]{1, 2}, new byte[]{3});

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
    "N/A",
    "95°C, 5g/100ml, 25+5s",
    true,
    Set.of("meetea-2025-jan", "meetea-2024-dec"))
    .setId(5);

  @Test
  void toCommand_translatesRecordToCommand() {
    var command = TeaRecordMapper.toCommand(USER_ID, TEA, IMAGES, VENDORS, TEA_TYPES, TAGS);

    assertEquals(TEA.getTitle(), command.title());
    assertEquals(TEA.getName(), command.name());
    assertEquals(TEA.getDescription(), command.description());
    assertEquals(TEA.getUrl(), command.url());
    assertEquals(TEA.getSeason(), command.scope().season());
    assertEquals(TEA.getCultivar(), command.scope().cultivar());
    assertEquals(TEA.getOrigin(), command.scope().origin());
    assertEquals(TEA.getElevation(), command.scope().elevation());
    assertNull(command.price());
    assertEquals(TEA.getBrewingInstructions(), command.brewingInstructions());
    assertEquals(TEA.isInStock(), command.inStock());
    assertEquals(USER_ID, command.userId());
    assertEquals(1L, command.vendorId());
    assertEquals(Set.of(7L, 8L), command.typeIds());
    assertEquals(Set.of(1L, 2L), command.tagIds());
    assertEquals(IMAGES, command.images());
  }

  @Test
  void toCommand_withPrice_parsesPrice() {
    var tea = withPrice(TEA, "12.5");
    var command = TeaRecordMapper.toCommand(USER_ID, tea, IMAGES, VENDORS, TEA_TYPES, TAGS);
    assertEquals(new Price(12.5f), command.price());
  }

  @Test
  void toCommand_invalidVendor_throws() {
    var tea = withVendor(TEA, "Meileaf");
    assertThrows(IllegalArgumentException.class,
      () -> TeaRecordMapper.toCommand(USER_ID, tea, IMAGES, VENDORS, TEA_TYPES, TAGS));
  }

  @Test
  void toCommand_invalidType_throws() {
    var tea = withTypes(TEA, Set.of("Dark", "Blend"));
    assertThrows(IllegalArgumentException.class,
      () -> TeaRecordMapper.toCommand(USER_ID, tea, IMAGES, VENDORS, TEA_TYPES, TAGS));
  }

  @Test
  void toCommand_invalidTag_throws() {
    var tea = withTags(TEA, Set.of("meetea-2025-jan", "unknown-tag"));
    assertThrows(IllegalArgumentException.class,
      () -> TeaRecordMapper.toCommand(USER_ID, tea, IMAGES, VENDORS, TEA_TYPES, TAGS));
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
      tea.getPrice(),
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
      tea.getPrice(),
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
      tea.getPrice(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tea.getTags()
    )
      .setId(tea.getId());
  }

  private static TeaRecord withPrice(TeaRecord tea, String price) {
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
      price,
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tea.getTags()
    )
      .setId(tea.getId());
  }
}
