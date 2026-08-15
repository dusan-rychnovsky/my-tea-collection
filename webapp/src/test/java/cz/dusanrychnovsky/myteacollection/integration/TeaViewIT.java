package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.containsStrings;
import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.Comparator.comparingInt;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class TeaViewIT {

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
  void teaView_showsGivenTea() throws Exception {
    var teaId = getTeaIdByTitle("Luminary Misfit");
    var actions = mvc.perform(get("/teas/" + teaId))
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
      "<span>95°C, 5g/100ml, 25+5s</span>",
      "<h2 class=\"tasting-notes-title\">Tasting notes</h2>",
      "<span class=\"tasting-notes-count\">4 tasting notes</span>",
      "<div class=\"tasting-notes-average-score\">4.4</div>",
      "<div class=\"tasting-notes-average-caption\">out of 5 · based on 4 tasting notes</div>",
      "<span class=\"dist-label\">0 <span class=\"dist-star\" aria-hidden=\"true\">★</span></span>",
      "<span class=\"tasting-note-author\">Ada K.</span>",
      "<span class=\"tasting-note-rating\">5.0</span>",
      "<span class=\"tasting-note-author\">Marek D.</span>",
      "<span class=\"tasting-note-rating\">4.5</span>",
      "<span class=\"tasting-note-author\">Lena V.</span>",
      "<span class=\"tasting-note-author\">Tomáš R.</span>",
      "<span class=\"tasting-note-rating\">4.0</span>");
  }

  @Test
  @Transactional
  void teaView_showsGivenTea_printsPrice() throws Exception {
    var teaId = getTeaIdByTitle("Doubleshot");
    var actions = mvc.perform(get("/teas/" + teaId))
      .andExpect(status().isOk());

    containsStrings(actions,
      "200 CZK / 50g");
  }

  @Test
  @Transactional
  void teaView_rendersOpenGraphMetaTags() throws Exception {
    var tea = teaRepository.findAll().stream()
      .filter(t -> t.getTitle().equals("Luminary Misfit"))
      .findFirst().orElseThrow();
    var mainImageId = tea.getImages().stream().min(comparingInt(TeaImageEntity::getIndex)).orElseThrow().getId();

    var actions = mvc.perform(get("/teas/" + tea.getId()))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<meta property=\"og:type\" content=\"website\"",
      "<meta property=\"og:site_name\" content=\"My Tea Collection\"",
      "<meta property=\"og:title\" content=\"Luminary Misfit\"",
      "<meta property=\"og:url\" content=\"http://localhost/teas/" + tea.getId() + "\"",
      "<meta property=\"og:description\" content=\"Ultra-fruity and fragrant PuErh",
      "<meta property=\"og:image\" content=\"http://localhost/images/" + mainImageId + "\"",
      "<meta name=\"twitter:card\" content=\"summary_large_image\"",
      "<title>Luminary Misfit — My tea collection</title>");
  }

  @Test
  @Transactional
  void teaView_rendersTags() throws Exception {
    var teaId = getTeaIdByTitle("Doubleshot");
    var actions = mvc.perform(get("/teas/" + teaId))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<li>meetea-2025-jan</li>",
      "<li>meetea-2024-dec</li>");
  }

  private Long getTeaIdByTitle(String title) {
    return teaRepository.findAll().stream()
      .filter(tea -> tea.getTitle().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB."))
      .getId();
  }
}
