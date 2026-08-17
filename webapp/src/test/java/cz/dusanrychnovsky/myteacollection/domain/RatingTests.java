package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
  void ofStars_wholeAndHalfValues_convertToHalfStars() {
    assertEquals(new Rating(0), Rating.ofStars(new BigDecimal("0.0")));
    assertEquals(new Rating(8), Rating.ofStars(new BigDecimal("4.0")));
    assertEquals(new Rating(9), Rating.ofStars(new BigDecimal("4.5")));
    assertEquals(new Rating(10), Rating.ofStars(new BigDecimal("5.0")));
  }

  @Test
  void ofStars_toleratesScaleVariants() {
    assertEquals(new Rating(8), Rating.ofStars(new BigDecimal("4")));
    assertEquals(new Rating(9), Rating.ofStars(new BigDecimal("4.50")));
  }

  @Test
  void ofStars_nonHalfStep_throws() {
    assertThrows(IllegalArgumentException.class, () -> Rating.ofStars(new BigDecimal("4.3")));
  }

  @Test
  void ofStars_outOfRange_throws() {
    assertThrows(IllegalArgumentException.class, () -> Rating.ofStars(new BigDecimal("5.5")));
    assertThrows(IllegalArgumentException.class, () -> Rating.ofStars(new BigDecimal("-0.5")));
  }

  @Test
  void ofStars_null_throws() {
    assertThrows(IllegalArgumentException.class, () -> Rating.ofStars(null));
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
