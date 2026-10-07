package cz.dusanrychnovsky.myteacollection.tea.query;

import java.util.Arrays;

public enum TeaSort {
  NEWEST("newest"),
  HIGHEST_SCORE("highest_score");

  private final String queryValue;

  TeaSort(String queryValue) {
    this.queryValue = queryValue;
  }

  public String queryValue() {
    return queryValue;
  }

  public static TeaSort fromQueryValue(String queryValue) {
    return Arrays.stream(values())
      .filter(sort -> sort.queryValue.equals(queryValue))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unknown tea sort: " + queryValue));
  }
}
