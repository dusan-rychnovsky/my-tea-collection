package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.TeaImageRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.containsStrings;
import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.doesNotContainStrings;
import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.TestInstance.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class TeaCollectionIT {

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private CreateUser createUser;

  @Autowired
  private MockMvc mvc;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private TeaImageRepository teaImageRepository;

  @BeforeEach
  void setup() throws IOException {
    // insert two teas in the DB
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
  }

  @Test
  @Transactional
  void index_listsFirstPageOfTeas() throws Exception {
    var actions = mvc.perform(get("/index?pageSize=2")
      .param("pageSize", "2"))
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

    verifyPagingMenu(actions, 3);
  }

  @Test
  @Transactional
  void index_listsSecondPageOfTeas() throws Exception {
    var actions = mvc.perform(get("/index")
      .param("pageNo", "1")
      .param("pageSize", "2"))
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

    verifyPagingMenu(actions, 3);
  }

  @Test()
  @Transactional
  void search_byNameOrTitle_listsRelevantTeas() throws Exception {
    var actions = mvc.perform(get("/search")
      .with(csrf())
      .param("query", "shou mei")
      .param("pageSize", "2"))
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

    verifyPagingMenu(actions, 2);
  }

  @Test
  @Transactional
  void search_byLocation_listsRelevantTeas() throws Exception {
    var actions = mvc.perform(get("/search")
        .with(csrf())
      .param("query", "yunnan"))
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

    doesNotContainStrings(
      actions,
      "Simple Dreams 2",
      "Shou Mei 2017"
    );
  }

  @Test
  @Transactional
  void filter_byType_listsRelevantTeas() throws Exception {
    var actions = mvc.perform(get("/filter")
      .with(csrf())
      .param("teaTypeId", "4")
      .param("vendorId", "2")
      .param("availabilityId", "0"))
      .andExpect(status().isOk());

    verifyHeader(actions);
    verifyDropdowns(actions);

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
      "Luminary Misfit",
      "Simple Dreams 2"
    );
  }

  @Test
  @Transactional
  void filter_byVendor_listsRelevantTeas() throws Exception {
    var actions = mvc.perform(get("/filter")
        .with(csrf())
        .param("teaTypeId", "0")
        .param("vendorId", "1")
        .param("availabilityId", "0")
        .param("pageSize", "2"))
      .andExpect(status().isOk());

    verifyHeader(actions);
    verifyDropdowns(actions);

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

    doesNotContainStrings(
      actions,
      "Douleshot",
      "Fujian Shoumei Bingcha 2017",
      "Jade Star 8"
    );

    verifyPagingMenu(actions, 2);
  }

  private void verifyHeader(ResultActions actions) throws Exception {
    containsStrings(actions,"<h1 class=\"jumbotron-heading\">My Tea Collection</h1>");
  }

  private void verifyDropdowns(ResultActions actions) throws Exception {
    containsStrings(actions,
      // verify tea type dropdown
      "<option value=\"1\" class=\"parent-tea-type\">Blend</option>",
      "<option value=\"7\" class=\"parent-tea-type\">Yellow Tea</option>",
      "<option value=\"15\" class=\"parent-tea-type\">Oolong</option>",
      "<option value=\"22\">Sheng Puerh</option>",
      "<option value=\"29\" class=\"parent-tea-type\">Yabao</option>",
      "<option value=\"25\">Fu Zhuan</option>",
      // verify vendor dropdown
      "<option value=\"4\">Lao Tea</option>",
      "<option value=\"5\">Klasek Tea</option>",
      "<option value=\"6\">Banna House</option>",
      // verify availability dropdown
      "<option value=\"1\">In stock</option>",
      "<option value=\"2\">Out of stock</option>"
    );
  }


  private void verifyPagingMenu(ResultActions actions, int numPages) throws Exception {
    for (int i = 1; i <= numPages; i++) {
      containsStrings(actions,
        "pageNo=" + (i - 1) + "\">" + i + "</a>"
      );
    }
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
}
