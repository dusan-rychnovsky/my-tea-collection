package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceTests {

  @Test
  void construct_negativeAmount_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Price(-0.01f));
  }

  @Test
  void label_formatsPricePer50gInCzk() {
    assertEquals("200 CZK / 50g", new Price(4f).label());
  }

  @Test
  void label_roundsFractionalResultToWholeCzk() {
    assertEquals("365 CZK / 50g", new Price(7.29f).label());
  }

  @Test
  void parse_validNumber_returnsPresentPrice() {
    assertEquals(Optional.of(new Price(12.5f)), Price.parse("12.5"));
  }

  @Test
  void parse_notAvailable_returnsEmpty() {
    assertTrue(Price.parse("N/A").isEmpty());
  }

  @Test
  void parse_invalidNumber_throws() {
    assertThrows(IllegalArgumentException.class, () -> Price.parse("twelve"));
  }

  @Test
  void parse_negativeNumber_throws() {
    assertThrows(IllegalArgumentException.class, () -> Price.parse("-5"));
  }
}
