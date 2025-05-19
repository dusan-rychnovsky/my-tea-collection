package cz.dusanrychnovsky.myteacollection;

import org.junit.jupiter.api.Test;

import static cz.dusanrychnovsky.myteacollection.model.Availability.toBoolean;
import static org.junit.jupiter.api.Assertions.*;

class AvailabilityTests {
  @Test
  void toBoolean_inStock_returnsTrue() {
    assertTrue(toBoolean(1L));
  }

  @Test
  void toBoolean_outOfStock_returnsFalse() {
    assertFalse(toBoolean(2L));
  }

  @Test
  void toBoolean_unexpectedValue_throws() {
    assertThrows(IllegalArgumentException.class, () -> toBoolean(0L));
    assertThrows(IllegalArgumentException.class, () -> toBoolean(3L));
  }
}
