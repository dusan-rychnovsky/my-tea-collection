package cz.dusanrychnovsky.myteacollection.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static cz.dusanrychnovsky.myteacollection.util.MapUtils.getOrThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MapUtilsTests {

  private final Map<Long, String> map = Map.of(
    1L, "one",
    2L, "two",
    3L, "three"
  );

  @Test
  public void getOrThrow_keyExists_returnsAssociatedValue() {
    assertEquals("two", getOrThrow(map, 2L));
  }

  @Test
  public void getOrThrow_keyDoesNotExist_throws() {
    assertThrows(IllegalArgumentException.class, () -> getOrThrow(map, 4L));
  }
}
