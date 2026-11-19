package cz.dusanrychnovsky.myteacollection.persistence;

public interface TeaResetOperations {

  /**
   * Deletes all teas and dependent data, restarting the tea identity.
   */
  void deleteAllTeas();
}
