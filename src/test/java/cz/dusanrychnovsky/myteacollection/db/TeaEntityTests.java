package cz.dusanrychnovsky.myteacollection.db;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeaEntityTests {

  @Test
  void getMainImage_NoImages_ReturnsEmptyResult() {
    var tea = new TeaEntity()
      .setImages(new HashSet<>());
    assertEquals(Optional.empty(), tea.getMainImage());
  }

  @Test
  void getMainImage_MultipleImages_ReturnsImageWithLowestIndex() {
    var images = Set.of(
      new TeaImageEntity().setIndex(1),
      new TeaImageEntity().setIndex(3),
      new TeaImageEntity().setIndex(2)
    );
    var tea = new TeaEntity()
      .setImages(images);
    assertEquals(1, tea.getMainImage().get().getIndex());
  }

  @Test
  void getAdditionalImages_NoImages_ReturnsEmptySet() {
    var tea = new TeaEntity();
    tea.setImages(new HashSet<>());
    assertTrue(tea.getAdditionalImages().isEmpty());
  }

  @Test
  void getAdditionalImages_MultipleImages_ReturnsAllImagesButTheOneWithLowestIndex() {
    var images = Set.of(
      new TeaImageEntity().setIndex(1),
      new TeaImageEntity().setIndex(3),
      new TeaImageEntity().setIndex(2)
    );
    var tea = new TeaEntity()
      .setImages(images);

    var result = tea.getAdditionalImages();
    assertEquals(2, tea.getAdditionalImages().size());
    for (var img : result) {
      assertTrue(img.getIndex() > 1);
    }
  }

  @Test
  void getUrlDomain_returnsDomainPortionOfTheUrl() throws MalformedURLException {
    var tea = new TeaEntity().setUrl("https://meileaf.com/tea/find-your-sunshine-2/");
    assertEquals("meileaf.com", tea.getUrlDomain());
  }

  @Test
  void printTypes_printsTypeNamesSortedByTypeIdsToString() {
    var types = Set.of(
      new TeaTypeEntity(1L, "Blend"),
      new TeaTypeEntity(9L, "Shu Puerh"),
      new TeaTypeEntity(7L, "Dark Tea")
    );
    var tea = new TeaEntity()
      .setTypes(types);

    assertEquals("Blend, Dark Tea, Shu Puerh", tea.printTypes());
  }

  @Test
  void printPrice_priceAvailable_printsPricePer50gInCzk() {
    var tea = new TeaEntity()
      .setPrice(4.f);
    assertEquals("200 CZK / 50g", tea.printPrice());
  }

  @Test
  void printPrice_priceMissing_printsNA() {
    var tea = new TeaEntity();
    assertEquals("N/A", tea.printPrice());
  }
}
