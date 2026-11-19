package cz.dusanrychnovsky.myteacollection.persistence;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class TeaResetOperationsImpl implements TeaResetOperations {

  private final EntityManager entityManager;

  public TeaResetOperationsImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  @Transactional
  public void deleteAllTeas() {
    entityManager.flush();
    for (var table : List.of(
      "TastingNotes", "Teas_Tags", "Teas_TeaTypes", "TeaImages", "TeaImageData", "Teas")) {
      entityManager.createNativeQuery("DELETE FROM myteacollection." + table).executeUpdate();
    }
    entityManager.createNativeQuery(
      "ALTER TABLE myteacollection.Teas ALTER COLUMN id RESTART WITH 1").executeUpdate();
    entityManager.clear();
  }
}
