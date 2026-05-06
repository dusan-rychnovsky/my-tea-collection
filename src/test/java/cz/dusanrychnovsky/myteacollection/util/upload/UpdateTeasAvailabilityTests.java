package cz.dusanrychnovsky.myteacollection.util.upload;

import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
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
    var teasById = Map.of(
      1L, entity(1L, true),
      2L, entity(2L, false));

    var result = computeUpdates(records, teasById);

    assertTrue(result.isEmpty());
  }

  @Test
  void computeUpdates_someDiffer_returnsOnlyDiffering() {
    var records = List.of(record(1L, true), record(2L, false), record(3L, true));
    var teasById = Map.of(
      1L, entity(1L, true),
      2L, entity(2L, true),
      3L, entity(3L, true));

    var result = computeUpdates(records, teasById);

    assertEquals(Map.of(2L, false), result);
  }

  @Test
  void computeUpdates_jsonTrueDbFalse_emitsTrue() {
    var records = List.of(record(1L, true));
    var teasById = Map.of(1L, entity(1L, false));

    var result = computeUpdates(records, teasById);

    assertEquals(Map.of(1L, true), result);
  }

  @Test
  void computeUpdates_jsonFalseDbTrue_emitsFalse() {
    var records = List.of(record(1L, false));
    var teasById = Map.of(1L, entity(1L, true));

    var result = computeUpdates(records, teasById);

    assertEquals(Map.of(1L, false), result);
  }

  @Test
  void computeUpdates_recordWithNoMatchingEntity_throws() {
    var records = List.of(record(42L, true));
    var teasById = Map.<Long, TeaEntity>of();

    var ex = assertThrows(IllegalStateException.class, () -> computeUpdates(records, teasById));
    assertTrue(ex.getMessage().contains("42"));
  }

  private static TeaRecord record(long id, boolean inStock) {
    return new TeaRecord(
      "title",
      "name",
      "description",
      Set.of("Dark"),
      "Vendor",
      "https://example.com",
      "origin",
      "cultivar",
      "season",
      "elevation",
      "N/A",
      "instructions",
      inStock,
      Set.of()
    ).setId(id);
  }

  private static TeaEntity entity(long id, boolean inStock) {
    return new StubTeaEntity(id, inStock);
  }

  private static class StubTeaEntity extends TeaEntity {
    private final long id;
    private final boolean inStock;

    StubTeaEntity(long id, boolean inStock) {
      this.id = id;
      this.inStock = inStock;
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public boolean isInStock() {
      return inStock;
    }
  }
}
