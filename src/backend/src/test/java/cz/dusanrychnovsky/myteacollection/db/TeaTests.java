package cz.dusanrychnovsky.myteacollection.db;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeaTests {

  @Test
  public void getMainImage_NoImages_ReturnsEmptyResult() {
    var tea = new Tea()
      .setImages(new HashSet<>());
    assertEquals(Optional.empty(), tea.getMainImage());
  }

  @Test
  public void getMainImage_MultipleImages_ReturnsImageWithLowestIndex() {
    var images = Set.of(
      new TeaImage().setIndex(1),
      new TeaImage().setIndex(3),
      new TeaImage().setIndex(2)
    );
    var tea = new Tea()
      .setImages(images);
    assertEquals(1, tea.getMainImage().get().getIndex());
  }

  @Test
  public void getAdditionalImages_NoImages_ReturnsEmptySet() {
    var tea = new Tea();
    tea.setImages(new HashSet<>());
    assertTrue(tea.getAdditionalImages().isEmpty());
  }

  @Test
  public void getAdditionalImages_MultipleImages_ReturnsAllImagesButTheOneWithLowestIndex() {
    var images = Set.of(
      new TeaImage().setIndex(1),
      new TeaImage().setIndex(3),
      new TeaImage().setIndex(2)
    );
    var tea = new Tea()
      .setImages(images);

    var result = tea.getAdditionalImages();
    assertEquals(2, tea.getAdditionalImages().size());
    for (var img : result) {
      assertTrue(img.getIndex() > 1);
    }
  }
}
