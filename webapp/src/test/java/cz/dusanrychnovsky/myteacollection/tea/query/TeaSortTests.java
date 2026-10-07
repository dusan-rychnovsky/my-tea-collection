package cz.dusanrychnovsky.myteacollection.tea.query;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TeaSortTests {

  @Test
  void fromQueryValue_mapsSupportedValues() {
    assertEquals(TeaSort.NEWEST, TeaSort.fromQueryValue("newest"));
    assertEquals(TeaSort.HIGHEST_SCORE, TeaSort.fromQueryValue("highest_score"));
  }

  @Test
  void fromQueryValue_rejectsUnknownValue() {
    assertThrows(IllegalArgumentException.class, () -> TeaSort.fromQueryValue("oldest"));
  }

  @Test
  void queryValue_returnsStableExternalValue() {
    assertEquals("newest", TeaSort.NEWEST.queryValue());
    assertEquals("highest_score", TeaSort.HIGHEST_SCORE.queryValue());
  }
}
