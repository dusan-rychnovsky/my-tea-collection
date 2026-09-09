package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonTests {

  @Test
  void year_exactYear_returnsYear() {
    assertEquals(Optional.of(2022), new Season("Spring 2022").year());
    assertEquals(Optional.of(2021), new Season("15th April 2021").year());
    assertEquals(Optional.of(2022), new Season("Spring and Autumn 2022").year());
  }

  @Test
  void construct_nullOrBlankValue_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Season(null));
    assertThrows(IllegalArgumentException.class, () -> new Season(""));
    assertThrows(IllegalArgumentException.class, () -> new Season("  "));
  }

  @Test
  void year_withoutExactYear_returnsEmpty() {
    assertTrue(new Season("Early 2000s").year().isEmpty());
    assertTrue(new Season("1990s").year().isEmpty());
    assertTrue(new Season("1899").year().isEmpty());
    assertTrue(new Season("2100").year().isEmpty());
    assertTrue(new Season("8582").year().isEmpty());
    assertTrue(new Season("Spring2022").year().isEmpty());
    assertTrue(new Season("2022abc").year().isEmpty());
  }

  @Test
  void year_repeatedSameYear_returnsSingleYear() {
    assertEquals(Optional.of(2022), new Season("Spring 2022 / Autumn 2022").year());
  }

  @Test
  void construct_multipleDistinctExactYears_throwsWithValueAndYears() {
    var exception = assertThrows(
      IllegalArgumentException.class,
      () -> new Season("Spring 2013, 2014 and 2018"));

    assertTrue(exception.getMessage().contains("Spring 2013, 2014 and 2018"));
    assertTrue(exception.getMessage().contains("2013"));
    assertTrue(exception.getMessage().contains("2014"));
    assertTrue(exception.getMessage().contains("2018"));
  }
}