package cz.dusanrychnovsky.myteacollection.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TeaScopeTests {

  @Test
  void construct_nullSeasonOptional_throws() {
    assertThrows(
      IllegalArgumentException.class,
      () -> new TeaScope(null, "Cultivar", "Origin", "1000m"));
  }
}