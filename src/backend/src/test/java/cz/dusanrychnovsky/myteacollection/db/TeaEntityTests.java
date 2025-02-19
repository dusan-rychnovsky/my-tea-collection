package cz.dusanrychnovsky.myteacollection.db;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeaEntityTests {

  @Test
  public void getMainImage_NoImages_ReturnsEmptyResult() {
    var tea = new TeaEntity()
      .setImages(new HashSet<>());
    assertEquals(Optional.empty(), tea.getMainImage());
  }

  @Test
  public void getMainImage_MultipleImages_ReturnsImageWithLowestIndex() {
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
  public void getAdditionalImages_NoImages_ReturnsEmptySet() {
    var tea = new TeaEntity();
    tea.setImages(new HashSet<>());
    assertTrue(tea.getAdditionalImages().isEmpty());
  }

  @Test
  public void getAdditionalImages_MultipleImages_ReturnsAllImagesButTheOneWithLowestIndex() {
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
  public void getUrlDomain_returnsDomainPortionOfTheUrl() throws MalformedURLException {
    var tea = new TeaEntity().setUrl("https://meileaf.com/tea/find-your-sunshine-2/");
    assertEquals("meileaf.com", tea.getUrlDomain());
  }
}
