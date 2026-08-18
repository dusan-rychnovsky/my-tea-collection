package cz.dusanrychnovsky.myteacollection.tea.query;

import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.VendorEntity;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaDetailTests {

  @Test
  void from_flattensScalarAndAssociationFields() {
    var tea = teaBuilder()
      .setTypes(Set.of(
        new TeaTypeEntity(7L, "Dark Tea"),
        new TeaTypeEntity(1L, "Blend"),
        new TeaTypeEntity(9L, "Shu Puerh")
      ));

    var detail = TeaDetail.from(tea);

    assertEquals("Luminary Misfit", detail.title());
    assertEquals("Lancang Gushu Sheng PuErh Spring 2022", detail.name());
    assertEquals("A fruity puerh.", detail.description());
    assertEquals("Mei Leaf", detail.vendorName());
    assertEquals(List.of("Blend", "Dark Tea", "Shu Puerh"), detail.typeNames());
    assertEquals("https://meileaf.com/tea/luminary-misfit/", detail.url());
    assertEquals("meileaf.com", detail.urlDomain());
    assertEquals("Spring 2022", detail.scope().season());
    assertEquals("Yunnan", detail.scope().origin());
    assertEquals("1740-1970m", detail.scope().elevation());
    assertEquals("Da Ye Zhong", detail.scope().cultivar());
    assertEquals("95C, 5g/100ml", detail.brewingInstructions());
  }

  @Test
  void from_sortsTagLabelsById() {
    var tea = teaBuilder();
    tea.setTypes(Set.of(new TeaTypeEntity(1L, "Blend")));
    ReflectionTestUtils.setField(tea, "tags", Set.of(
      new TagEntity(3L, null, "winter", null),
      new TagEntity(1L, null, "autumn", null)
    ));

    assertEquals(List.of("autumn", "winter"), TeaDetail.from(tea).tagLabels());
  }

  @Test
  void from_priceAvailable_formatsPricePer50gInCzk() {
    var tea = teaBuilder().setPrice(4.f);
    assertEquals("200 CZK / 50g", TeaDetail.from(tea).priceLabel());
  }

  @Test
  void from_priceMissing_priceLabelIsNA() {
    var tea = teaBuilder().setPrice(null);
    assertEquals("N/A", TeaDetail.from(tea).priceLabel());
  }

  @Test
  void from_malformedUrl_throwsIllegalArgumentException() {
    var tea = teaBuilder().setUrl("not a valid url");
    assertThrows(IllegalArgumentException.class, () -> TeaDetail.from(tea));
  }

  @Test
  void from_noImages_mainImageIdNullAndNoAdditional() {
    var detail = TeaDetail.from(teaBuilder());

    assertNull(detail.mainImageId());
    assertTrue(detail.additionalImageIds().isEmpty());
  }

  @Test
  void from_multipleImages_mainIsLowestIndexAndAdditionalOrderedByIndex() {
    var tea = teaBuilder();
    tea.setImages(Set.of(
      image(100L, 2),
      image(200L, 1),
      image(300L, 3)
    ));

    var detail = TeaDetail.from(tea);

    assertEquals(200L, detail.mainImageId().longValue());
    assertEquals(List.of(100L, 300L), detail.additionalImageIds());
  }

  private TeaEntity teaBuilder() {
    return new TeaEntity(
      null,
      new VendorEntity(1L, "Mei Leaf", null),
      Set.of(new TeaTypeEntity(1L, "Blend")),
      "Luminary Misfit",
      "Lancang Gushu Sheng PuErh Spring 2022",
      "A fruity puerh.",
      "https://meileaf.com/tea/luminary-misfit/",
      new TeaScopeEntity("Spring 2022", "Da Ye Zhong", "Yunnan", "1740-1970m"),
      null,
      "95C, 5g/100ml",
      true,
      Set.of()
    );
  }

  private TeaImageEntity image(long id, int index) {
    var image = new TeaImageEntity().setIndex(index);
    ReflectionTestUtils.setField(image, "id", id);
    return image;
  }
}
