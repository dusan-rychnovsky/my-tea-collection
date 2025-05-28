package cz.dusanrychnovsky.myteacollection.model;

public record SearchCriteria(String query) {
  public static SearchCriteria EMPTY = new SearchCriteria("");
}
