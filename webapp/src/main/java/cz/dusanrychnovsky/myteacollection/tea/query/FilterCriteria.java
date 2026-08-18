package cz.dusanrychnovsky.myteacollection.tea.query;

public record FilterCriteria(int teaTypeId, int vendorId, int availabilityId) {
  public static FilterCriteria EMPTY = new FilterCriteria(0, 0, 0);
}
