package cz.dusanrychnovsky.myteacollection.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class PageInfoTests {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5})
  public void fewPagesOnly_showsAllPagesWithoutEllipsis(int numPages) {
    var expectedPages = IntStream.range(0, numPages).boxed().toList();
    for (int currPageNo = 0; currPageNo < numPages; currPageNo++) {
      var pageInfo = new PageInfo(currPageNo, numPages);
      assertEquals(expectedPages, pageInfo.getVisiblePages());
      assertFalse(pageInfo.showStartEllipsis());
      assertFalse(pageInfo.showEndEllipsis());
    }
  }

  @Test
  public void manyPages_onFirstPage_showsFirstPagesWithEndEllipsis() {
    PageInfo pageInfo = new PageInfo(0, 10);
    assertEquals(List.of(0, 1, 2, 3, 4), pageInfo.getVisiblePages());
    assertFalse(pageInfo.showStartEllipsis());
    assertTrue(pageInfo.showEndEllipsis());
  }

  @Test
  public void manyPages_onSecondPage_showsFirstPagesWithEndEllipsis() {
    PageInfo pageInfo = new PageInfo(1, 10);
    assertEquals(List.of(0, 1, 2, 3, 4), pageInfo.getVisiblePages());
    assertFalse(pageInfo.showStartEllipsis());
    assertTrue(pageInfo.showEndEllipsis());
  }

  @Test
  public void manyPages_onThirdPage_showsFirstPagesWithEndEllipsis() {
    PageInfo pageInfo = new PageInfo(2, 10);
    assertEquals(List.of(0, 1, 2, 3, 4), pageInfo.getVisiblePages());
    assertFalse(pageInfo.showStartEllipsis());
    assertTrue(pageInfo.showEndEllipsis());
  }

  @Test
  public void manyPages_onLastPage_showsLastPagesWithStartEllipsis() {
    PageInfo pageInfo = new PageInfo(9, 10);
    assertEquals(List.of(5, 6, 7, 8, 9), pageInfo.getVisiblePages());
    assertTrue(pageInfo.showStartEllipsis());
    assertFalse(pageInfo.showEndEllipsis());
  }

  @Test
  public void manyPages_onPenultimatePage_showsLastPagesWithStartEllipsis() {
    PageInfo pageInfo = new PageInfo(8, 10);
    assertEquals(List.of(5, 6, 7, 8, 9), pageInfo.getVisiblePages());
    assertTrue(pageInfo.showStartEllipsis());
    assertFalse(pageInfo.showEndEllipsis());
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 4, 5, 6})
  public void manyPages_onMiddlePage_showsFewPagesBeforeAndAfterAndBothEllipses(int currPageNo) {
    PageInfo pageInfo = new PageInfo(currPageNo, 10);
    assertEquals(IntStream.range(currPageNo - 2, currPageNo + 3).boxed().toList(), pageInfo.getVisiblePages());
    assertTrue(pageInfo.showStartEllipsis());
    assertTrue(pageInfo.showEndEllipsis());
  }
}
