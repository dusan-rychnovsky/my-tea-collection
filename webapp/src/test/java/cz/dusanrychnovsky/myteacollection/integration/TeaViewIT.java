package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.containsStrings;
import static cz.dusanrychnovsky.myteacollection.integration.ITUtils.doesNotContainStrings;
import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.Comparator.comparingInt;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  private UserRepository userRepository;

  @Autowired
  private TastingNoteRepository tastingNoteRepository;

  @BeforeEach
  void setup() throws IOException {
    // insert teas in the DB
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
    seedTastingNotes();
  }

  private void seedTastingNotes() {
    var owner = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow();

    // "Luminary Misfit" gets four notes reproducing the design mockup (5.0, 4.5, 4.0, 4.0).
    var luminary = teaByTitle("Luminary Misfit");
    tastingNoteRepository.saveAll(List.of(
      new TastingNoteEntity(luminary, owner, 10, LocalDate.of(2026, 7, 21),
        "Clean, layered and quietly energizing.\n\nOne of my favourite sessions this year."),
      new TastingNoteEntity(luminary, owner, 9, LocalDate.of(2026, 6, 8),
        "Thick, almost syrupy texture with ripe stone-fruit notes."),
      new TastingNoteEntity(luminary, owner, 8, LocalDate.of(2026, 5, 19),
        "Very fragrant and a lovely first impression."),
      new TastingNoteEntity(luminary, owner, 8, LocalDate.of(2026, 4, 30),
        "A dependable daily drinker.")
    ));

    // "Shou Mei 2017" gets a single note whose body contains HTML, to prove it is escaped.
    var shouMei = teaByTitle("Shou Mei 2017");
    tastingNoteRepository.save(new TastingNoteEntity(shouMei, owner, 6, LocalDate.of(2026, 3, 15),
      "Contains <b>bold</b> & <script>evil</script> markup."));

    // "Doubleshot" is deliberately left without notes (empty-state coverage).
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
      "<span>95°C, 5g/100ml, 25+5s</span>");
  }

  @Test
  @Transactional
  void teaView_rendersTastingNotesSummaryAndList() throws Exception {
    var teaId = getTeaIdByTitle("Luminary Misfit");
    var actions = mvc.perform(get("/teas/" + teaId))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<h2 class=\"tasting-notes-title\">Tasting notes</h2>",
      "<span class=\"tasting-notes-count\">4 tasting notes</span>",
      "<div class=\"tasting-notes-average-score\">4.4</div>",
      "<div class=\"tasting-notes-average-caption\">out of 5 · based on 4 tasting notes</div>",
      // six-row distribution, newly including the 0★ row; 5★ and 4★ each hold two notes
      "<span class=\"dist-label\">5 <span class=\"dist-star\" aria-hidden=\"true\">★</span></span>",
      "<span class=\"dist-label\">0 <span class=\"dist-star\" aria-hidden=\"true\">★</span></span>",
      "--pct: 50%;",
      // notes are attributed to the owning user, newest first
      "<span class=\"tasting-note-author\">Dušan R.</span>",
      "<span class=\"tasting-note-date\">21 Jul 2026</span>",
      "aria-hidden=\"true\">DR</div>",
      "<span class=\"tasting-note-rating\">5.0</span>",
      "<span class=\"tasting-note-rating\">4.5</span>",
      "<span class=\"tasting-note-rating\">4.0</span>",
      // body split into paragraphs
      "<p>Clean, layered and quietly energizing.</p>",
      "<p>One of my favourite sessions this year.</p>");

    // the hard-coded mockup reviewers must be gone
    doesNotContainStrings(actions,
      "Ada K.", "Marek D.", "Lena V.", "Tomáš R.");
  }

  @Test
  @Transactional
  void teaView_ordersTastingNotesNewestThenByIdDescending() throws Exception {
    var owner = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow();
    var tea = teaByTitle("Jade Star 8");
    tastingNoteRepository.saveAll(List.of(
      new TastingNoteEntity(tea, owner, 6, LocalDate.of(2026, 2, 1), "Older by date."),
      new TastingNoteEntity(tea, owner, 7, LocalDate.of(2026, 2, 10), "Newest by date."),
      new TastingNoteEntity(tea, owner, 8, LocalDate.of(2026, 2, 10), "Same date, inserted later.")
    ));

    var body = mvc.perform(get("/teas/" + tea.getId()))
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    // most recent date first; within the same date, the higher (later-inserted) id wins
    assertTrue(body.indexOf("Same date, inserted later.") < body.indexOf("Newest by date."));
    assertTrue(body.indexOf("Newest by date.") < body.indexOf("Older by date."));
  }

  @Test
  @Transactional
  void teaView_noTastingNotes_showsEmptyState() throws Exception {
    var teaId = getTeaIdByTitle("Doubleshot");
    var actions = mvc.perform(get("/teas/" + teaId))
      .andExpect(status().isOk());

    containsStrings(actions,
      "<h2 class=\"tasting-notes-title\">Tasting notes</h2>",
      "<p class=\"tasting-notes-empty\">This tea is still waiting for its first tasting note.</p>");
    doesNotContainStrings(actions,
      "tasting-notes-average-score",
      "class=\"tasting-note-list\"");
  }

  @Test
  @Transactional
  void teaView_escapesTastingNoteMarkup() throws Exception {
    var teaId = getTeaIdByTitle("Shou Mei 2017");
    var actions = mvc.perform(get("/teas/" + teaId))
      .andExpect(status().isOk());

    containsStrings(actions,
      "Contains &lt;b&gt;bold&lt;/b&gt; &amp; &lt;script&gt;evil&lt;/script&gt; markup.");
    doesNotContainStrings(actions,
      "<b>bold</b>",
      "<script>evil</script>");
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

  private TeaEntity teaByTitle(String title) {
    return teaRepository.findAll().stream()
      .filter(tea -> tea.getTitle().equals(title))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB."));
  }

  private Long getTeaIdByTitle(String title) {
    return teaByTitle(title).getId();
  }
}
