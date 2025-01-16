package cz.dusanrychnovsky.myteacollection.model;

import java.util.List;

public record Availability(Long id, String label) {
  public static List<Availability> getAll() {
    return List.of(
      new Availability(0L, "All"),
      new Availability(1L, "In stock"),
      new Availability(2L, "Out of stock")
    );
  }
}
