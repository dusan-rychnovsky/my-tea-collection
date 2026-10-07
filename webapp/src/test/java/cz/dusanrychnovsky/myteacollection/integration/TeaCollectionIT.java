package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TeaImageRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
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
import java.util.List;

import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.containsStrings;
import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.doesNotContainStrings;
import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.TestInstance.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
      "Long Description Fixture",
      "",
      "Mei Leaf",
      "Dark Tea"
    );
    verifyTea(
      actions,
      "Approximate Season Fixture",
      "",
      "Mei Leaf",
      "Dark Tea"
    );

    verifyPagingMenu(actions, 4);
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
      "Jade Star 8",
      "2013/2014 Fuding Bai Mu Dan &amp; 2018 Pan Xi Shou Mei",
      "Mei Leaf",
      "Blend, White Tea, Bai Mu Dan, Shou Mei"
    );
    verifyTea(
      actions,
      "Shou Mei 2017",
      "Fujian Shoumei Bingcha 2017",
      "Meetea",
      "White Tea"
    );

    verifyPagingMenu(actions, 4);
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
      "Jade Star 8",
      "2013/2014 Fuding Bai Mu Dan &amp; 2018 Pan Xi Shou Mei",
      "Mei Leaf",
      "Blend, White Tea, Bai Mu Dan, Shou Mei"
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
      "Doubleshot",
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
      "Doubleshot",
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
      "Long Description Fixture",
      "",
      "Mei Leaf",
      "Dark Tea"
    );
    verifyTea(
      actions,
      "Approximate Season Fixture",
      "",
      "Mei Leaf",
      "Dark Tea"
    );

    doesNotContainStrings(
      actions,
      "Doubleshot",
      "Fujian Shoumei Bingcha 2017",
      "Jade Star 8",
      "Luminary Misfit"
    );

    verifyPagingMenu(actions, 3);
  }

  @Test
  @Transactional
  void index_rendersCanonicalDetailsLinksOnTeaImageTitleAndViewButton() throws Exception {
    var teaSlug = getTeaSlugByTitle("Doubleshot");
    var actions = mvc.perform(get("/index")
      .param("pageSize", "9"))
      .andExpect(status().isOk());

    containsStrings(actions,
      "href=\"/teas/" + teaSlug + "\" class=\"tea-image-link tea-details-link\"",
      "href=\"/teas/" + teaSlug + "\" class=\"tea-title-link tea-details-link\"",
      "formaction=\"/teas/" + teaSlug
        + "\" type=\"submit\" class=\"btn btn-sm btn-outline-secondary\">View</button>"
    );
  }

  @Test
  @Transactional
  void index_rendersHalfStarRatingPreviewOnEveryCard() throws Exception {
    var actions = mvc.perform(get("/index").param("pageSize", "2"))
      .andExpect(status().isOk());

    containsStrings(actions,
      "class=\"tea-card-rating\" role=\"img\" aria-label=\"Rating preview: 4.5 out of 5\"",
      "<span class=\"stars\" style=\"--rating: 4.5;\" aria-hidden=\"true\">",
      "<span class=\"stars-fill\"></span>"
    );

    var html = actions.andReturn().getResponse().getContentAsString();
    assertEquals(2, html.split("class=\"tea-card-rating\"", -1).length - 1);
  }

  @Test
  @Transactional
  void index_rendersSortControlWithNewestTeaSelectedByDefault() throws Exception {
    var actions = mvc.perform(get("/index"))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<label for=\"select-tea-sort\">Sort by:</label>",
      "<option value=\"newest\" selected=\"selected\">Newest first</option>",
      "<option value=\"highest_score\">Highest score first</option>"
    );
  }

  @Test
  @Transactional
  void index_highestScoreSort_ordersRatedTeasAndKeepsSelectionInPagingLinks() throws Exception {
    var doubleshot = teaByTitle("Doubleshot").setAverageRatingHalfStars(10.0);
    var luminary = teaByTitle("Luminary Misfit").setAverageRatingHalfStars(8.0);
    teaRepository.saveAllAndFlush(List.of(doubleshot, luminary));

    var actions = mvc.perform(get("/index")
        .param("sort", "highest_score")
        .param("pageSize", "2"))
      .andExpect(status().isOk());

    verifyTeaOrder(actions, "Doubleshot", "Luminary Misfit");
    containsStrings(actions,
      "<option value=\"highest_score\" selected=\"selected\">Highest score first</option>",
      "<input type=\"hidden\" name=\"sort\" value=\"highest_score\">",
      "sort=highest_score&amp;pageNo=1"
    );
  }

  @Test
  @Transactional
  void index_unknownSort_returnsBadRequest() throws Exception {
    mvc.perform(get("/index").param("sort", "oldest"))
      .andExpect(status().isBadRequest());
  }

  private void verifyHeader(ResultActions actions) throws Exception {
    containsStrings(actions,"<h1 class=\"jumbotron-heading\">My Tea Collection</h1>");
  }

  private void verifyDropdowns(ResultActions actions) throws Exception {
    containsStrings(actions,
      // verify tea type dropdown
      "<option value=\"1\" class=\"parent-tea-type\">Blend</option>",
      "<option value=\"7\" class=\"parent-tea-type\">Yellow Tea</option>",
      "<option value=\"17\" class=\"parent-tea-type\">Oolong</option>",
      "<option value=\"22\">GABA</option>",
      "<option value=\"26\">Sheng Puerh</option>",
      "<option value=\"33\" class=\"parent-tea-type\">Yabao</option>",
      "<option value=\"29\">Fu Zhuan</option>",
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

  private void verifyTeaOrder(ResultActions actions, String firstTitle, String secondTitle)
    throws Exception {

    var html = actions.andReturn().getResponse().getContentAsString();
    var firstIndex = html.indexOf("<span>" + firstTitle + "</span>");
    var secondIndex = html.indexOf("<span>" + secondTitle + "</span>");
    assertTrue(firstIndex >= 0);
    assertTrue(secondIndex > firstIndex);
  }

  private String getTeaSlugByTitle(String title) {
    return teaByTitle(title).getSlug();
  }

  private TeaEntity teaByTitle(String title) {
    return teaRepository.findAll().stream()
      .filter(tea -> tea.getTitle().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB."));
  }
}
