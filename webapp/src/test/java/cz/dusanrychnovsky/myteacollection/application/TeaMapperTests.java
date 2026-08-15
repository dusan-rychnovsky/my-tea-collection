package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.VendorEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.Tea;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static java.util.Comparator.comparingInt;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaMapperTests {

  private static final UserEntity USER = new UserEntity(
    "dusan.rychnovsky@gmail.com", "pwd", "Dušan", "Rychnovský", null, "Prague, Czech republic", null);
  private static final VendorEntity VENDOR = new VendorEntity(1L, "Mei Leaf", "https://meileaf.com");
  private static final Set<TeaTypeEntity> TYPES = Set.of(new TeaTypeEntity(25L, "Sheng Puerh"));
  private static final Set<TagEntity> TAGS = Set.of(new TagEntity(1L, USER, "meetea-2025-jan", "desc"));

  private static Tea tea(Price price, List<byte[]> images) {
    return new Tea(
      "Luminary Misfit",
      "Lancang Gushu Sheng PuErh",
      "Ultra-fruity and fragrant PuErh.",
      "https://meileaf.com/tea/luminary-misfit/",
      new TeaScope("April 2022", "Da Ye Zhong", "Lancang", "1740-1970m"),
      price,
      "95°C, 5g/100ml, 25+5s",
      true,
      1L,
      Set.of(25L),
      Set.of(1L),
      images);
  }

  @Test
  void toEntity_mapsDescriptiveFieldsAndReferences() {
    var entity = TeaMapper.toEntity(tea(new Price(7.29f), List.of(new byte[]{1})), USER, VENDOR, TYPES, TAGS);

    assertNull(entity.getId());
    assertEquals(USER, entity.getUser());
    assertEquals(VENDOR, entity.getVendor());
    assertEquals(TYPES, entity.getTypes());
    assertEquals(TAGS, entity.getTags());
    assertEquals("Luminary Misfit", entity.getTitle());
    assertEquals("Lancang Gushu Sheng PuErh", entity.getName());
    assertEquals("Ultra-fruity and fragrant PuErh.", entity.getDescription());
    assertEquals("https://meileaf.com/tea/luminary-misfit/", entity.getUrl());
    assertEquals("April 2022", entity.getScope().getSeason());
    assertEquals("Da Ye Zhong", entity.getScope().getCultivar());
    assertEquals("Lancang", entity.getScope().getOrigin());
    assertEquals("1740-1970m", entity.getScope().getElevation());
    assertEquals(7.29f, entity.getPrice());
    assertEquals("95°C, 5g/100ml, 25+5s", entity.getBrewingInstructions());
    assertTrue(entity.isInStock());
  }

  @Test
  void toEntity_nullPrice_mapsToNullEntityPrice() {
    assertNull(TeaMapper.toEntity(tea(null, List.of(new byte[]{1})), USER, VENDOR, TYPES, TAGS).getPrice());
  }

  @Test
  void toEntity_buildsOrderedImagesWithOneBasedIndex() {
    var first = new byte[]{1, 2, 3};
    var second = new byte[]{4, 5};

    var entity = TeaMapper.toEntity(tea(null, List.of(first, second)), USER, VENDOR, TYPES, TAGS);

    var images = entity.getImages().stream().sorted(comparingInt(TeaImageEntity::getIndex)).toList();
    assertEquals(2, images.size());
    assertEquals(1, images.get(0).getIndex());
    assertArrayEquals(first, images.get(0).getData());
    assertEquals(2, images.get(1).getIndex());
    assertArrayEquals(second, images.get(1).getData());
  }
}
