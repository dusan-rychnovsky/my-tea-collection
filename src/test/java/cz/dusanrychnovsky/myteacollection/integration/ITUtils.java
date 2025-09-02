package cz.dusanrychnovsky.myteacollection.integration;

import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

public class ITUtils {

  public static void containsStrings(ResultActions actions, String... strings)
    throws Exception {
    for (var str : strings) {
      actions.andExpect(content().string(
        containsString(str)
      ));
    }
  }

  public static void doesNotContainStrings(ResultActions actions, String... strings)
    throws Exception {
    for (var str : strings) {
      actions.andExpect(content().string(
        not(containsString(str))
      ));
    }
  }
}
