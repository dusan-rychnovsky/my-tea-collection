package cz.dusanrychnovsky.myteacollection.util.upload;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.upload.UpdateTeasAvailability.computeUpdates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateTeasAvailabilityTests {

  @Test
  void computeUpdates_allMatch_returnsEmpty() {
    var records = List.of(record(1L, true), record(2L, false));
    var dbInStockById = Map.of(1L, true, 2L, false);

    var result = computeUpdates(records, dbInStockById);

    assertTrue(result.isEmpty());
  }

  @Test
  void computeUpdates_someDiffer_returnsOnlyDiffering() {
    var records = List.of(record(1L, true), record(2L, false), record(3L, true));
    var dbInStockById = Map.of(
      1L, true,
      2L, true,
      3L, true);

    var result = computeUpdates(records, dbInStockById);

    assertEquals(Map.of(2L, false), result);
  }

  @Test
  void computeUpdates_jsonTrueDbFalse_emitsTrue() {
    var records = List.of(record(1L, true));
    var dbInStockById = Map.of(1L, false);

    var result = computeUpdates(records, dbInStockById);

    assertEquals(Map.of(1L, true), result);
  }

  @Test
  void computeUpdates_jsonFalseDbTrue_emitsFalse() {
    var records = List.of(record(1L, false));
    var dbInStockById = Map.of(1L, true);

    var result = computeUpdates(records, dbInStockById);

    assertEquals(Map.of(1L, false), result);
  }

  @Test
  void computeUpdates_recordWithNoMatchingEntity_throws() {
    var records = List.of(record(42L, true));
    var dbInStockById = Map.<Long, Boolean>of();

    var ex = assertThrows(IllegalStateException.class, () -> computeUpdates(records, dbInStockById));
    assertTrue(ex.getMessage().contains("42"));
  }

  private static TeaRecord record(long id, boolean inStock) {
    return new TeaRecord(
      "title",
      "name",
      "description",
      Set.of("tea type"),
      "vendor",
      "url",
      "origin",
      "cultivar",
      "season",
      "elevation",
      "price",
      "instructions",
      inStock,
      Set.of()
    ).setId(id);
  }
}
