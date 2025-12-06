package cz.dusanrychnovsky.myteacollection.model;

import java.util.List;
import java.util.stream.IntStream;

public record PageInfo(
  int currentPageNo,
  int totalPages
) {
  private static final int MAX_VISIBLE_PAGES = 5;
  private static final int PAGES_AROUND_CURRENT = MAX_VISIBLE_PAGES / 2;

  /**
   * Calculates which page numbers should be visible in the pagination menu. Which is -
   * max. {@value MAX_VISIBLE_PAGES} pages, centered around the current page.
   *
   * @return A list of page numbers to display (0-based).
   */
  public List<Integer> getVisiblePages() {
    var minPage = currentPageNo - PAGES_AROUND_CURRENT;
    var maxPage = currentPageNo + PAGES_AROUND_CURRENT;
    while (minPage < 0) {
      minPage++;
      maxPage++;
    }
    while (maxPage >= totalPages) {
      minPage--;
      maxPage--;
    }
    if (minPage < 0) {
      minPage = 0;
    }
    return IntStream.range(minPage, maxPage + 1)
      .boxed()
      .toList();
  }

  /**
   * Check if ellipsis should be shown at the start of the pagination menu, which is whenever
   * there are more pages to the left of the visible page range.
   */
  public boolean showStartEllipsis() {
    return totalPages > MAX_VISIBLE_PAGES &&
      currentPageNo > 2;
  }

  /**
   * Checks if ellipsis should be shown at the end of the pagination menu, which is whenever
   * there are more pages to the right of the visible page range.
   */
  public boolean showEndEllipsis() {
    return totalPages > MAX_VISIBLE_PAGES &&
      currentPageNo < totalPages - 1 - PAGES_AROUND_CURRENT;
  }
}
