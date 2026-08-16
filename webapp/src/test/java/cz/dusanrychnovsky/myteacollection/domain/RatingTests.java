package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatingTests {

  @Test
  void construct_belowRange_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Rating(-1));
  }

  @Test
  void construct_aboveRange_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Rating(11));
  }

  @Test
  void value_convertsHalfStarsToStars() {
    assertEquals(0.0, new Rating(0).value());
    assertEquals(4.5, new Rating(9).value());
    assertEquals(5.0, new Rating(10).value());
  }

  @Test
  void roundedStars_roundsHalfUp() {
    assertEquals(0, new Rating(0).roundedStars());
    assertEquals(1, new Rating(1).roundedStars());
    assertEquals(4, new Rating(8).roundedStars());
    assertEquals(5, new Rating(9).roundedStars());
    assertEquals(5, new Rating(10).roundedStars());
  }

  @Test
  void label_formatsOneDecimalInEnglish() {
    assertEquals("0.0", new Rating(0).label());
    assertEquals("4.5", new Rating(9).label());
    assertEquals("5.0", new Rating(10).label());
  }
}
