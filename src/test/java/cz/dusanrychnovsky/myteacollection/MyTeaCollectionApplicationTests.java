package cz.dusanrychnovsky.myteacollection;

import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.TestInstance.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class MyTeaCollectionApplicationTests {

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private MockMvc mvc;

  @BeforeAll
  void setup() throws IOException {
    // insert two teas in the DB
    uploadNewTeas.run(toFile("teas"));
  }

  @Test
  void index_listsAllTeas() throws Exception {
    var actions = mvc.perform(get("/index"))
      .andExpect(status().isOk());

    verifyHeader(actions);
    verifyDropdowns(actions);

    verifyTea(
      actions,
      "Doubleshot",
      "Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022",
      "Meetea",
      "Dark Tea, Shu Puerh"
    );
    verifyTea(
      actions,
      "Luminary Misfit",
      "Lancang Gushu Sheng PuErh Spring 2022",
      "Mei Leaf",
      "Dark Tea, Sheng Puerh"
    );
    verifyTea(
      actions,
      "Simple Dreams 2",
      "2021 Zhenghe Shou Mei Blend",
      "Mei Leaf",
      "Blend, White Tea"
    );
    verifyTea(
      actions,
      "Shou Mei 2017",
      "Fujian Shoumei Bingcha 2017",
      "Meetea",
      "White Tea"
    );
  }

  @Test
  void teaView_showsGivenTea() throws Exception {
    var actions = mvc.perform(get("/teas/2"))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<span>Luminary Misfit</span>",
      "<div class=\"subtitle mt-1\">Lancang Gushu Sheng PuErh Spring 2022</div>",
      "<li>Dark Tea</li>",
      "<li>Sheng Puerh</li>",
      "<span>Mei Leaf</span>",
      "<a href=\"https://meileaf.com/tea/luminary-misfit/\">meileaf.com</a>",
      "<span>April 2022</span>",
      "<span>Lancang, Puer, Yunnan, China</span>",
      "<span>1740-1970m</span>",
      "<span>Da Ye Zhong</span>",
      "<span>95°C, 5g/100ml, 25+5s</span>");
  }

  @Disabled()
  @Test()
  void search_byNameOrTitle_listsRelevantTeas() throws Exception {
    var actions = mvc.perform(post("/search", new SearchCriteria("shou mei")))
      .andExpect(status().isOk());

    verifyHeader(actions);
    verifyDropdowns(actions);

    verifyTea(
      actions,
      "Simple Dreams 2",
      "2021 Zhenghe Shou Mei Blend",
      "Mei Leaf",
      "Blend, White Tea"
    );
    verifyTea(
      actions,
      "Shou Mei 2017",
      "Fujian Shoumei Bingcha 2017",
      "Meetea",
      "White Tea"
    );

    doesNotContainStrings(
      actions,
      "Douleshot",
      "Luminary Misfit"
    );
  }

  private void verifyHeader(ResultActions actions) throws Exception {
    containsStrings(actions,"<h1 class=\"jumbotron-heading\">My Tea Collection</h1>");
  }

  private void verifyDropdowns(ResultActions actions) throws Exception {
    containsStrings(actions,
      // verify tea type dropdown
      "<option value=\"1\">Blend</option>",
      "<option value=\"2\">White Tea</option>",
      "<option value=\"5\">Oolong</option>",
      "<option value=\"8\">Sheng Puerh</option>",
      "<option value=\"11\">Yabao</option>",
      "<option value=\"12\">Fu Zhuan</option>",
      // verify vendor dropdown
      "<option value=\"1\">Mei Leaf</option>",
      "<option value=\"2\">Meetea</option>",
      "<option value=\"5\">Klasek Tea</option>",
      "<option value=\"6\">Banna House</option>",
      // verify availability dropdown
      "<option value=\"1\">In stock</option>",
      "<option value=\"2\">Out of stock</option>"
    );
  }

  private void verifyTea(
    ResultActions actions, String title, String name, String vendor, String types)
    throws Exception {

    containsStrings(actions,
      "<span>" + title + "</span>",
      "<span>" + name + "</span>",
      "<span>" + vendor + "</span>",
      "<span>" + types + "</span>"
    );
  }

  private void containsStrings(ResultActions actions, String... strings) throws Exception {
    for (var str : strings) {
      actions.andExpect(content().string(
        containsString(str)
      ));
    }
  }

  private void doesNotContainStrings(ResultActions actions, String... strings) throws Exception {
    for (var str : strings) {
      actions.andExpect(content().string(
        not(containsString(str))
      ));
    }
  }
}
