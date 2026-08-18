package cz.dusanrychnovsky.myteacollection.tea.query;

public record SearchCriteria(String query) {
  public static SearchCriteria EMPTY = new SearchCriteria("");
}
