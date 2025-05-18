package cz.dusanrychnovsky.myteacollection.util;

import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class MapUtils {

  private MapUtils() {
    throw new IllegalStateException("Utility class.");
  }

  public static <K, V> V getOrThrow(Map<K, V> map, K key) {
    var result = map.get(key);
    if (result == null) {
      throw new IllegalArgumentException("Unknown key: " + key);
    }
    return result;
  }

  public static <K, V> Set<V> mapAll(Map<K, V> map, Set<K> keys) {
    return keys.stream()
      .map(key -> getOrThrow(map, key))
      .collect(toSet());
  }
}
