package cz.dusanrychnovsky.myteacollection.util;

import java.util.Map;

public class MapUtils {
  public static <K, V> V getOrThrow(Map<K, V> map, K key) {
    var result = map.get(key);
    if (result == null) {
      throw new IllegalArgumentException("Unknown key: " + key);
    }
    return result;
  }
}
