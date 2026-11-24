package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.tastingnotes.ingest.UploadTastingNotes;
import cz.dusanrychnovsky.myteacollection.tea.ingest.CannotLoadTeaInfoException;
import cz.dusanrychnovsky.myteacollection.tea.ingest.TeaRecord;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.JpgCompression;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest()
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties", properties =
  "spring.datasource.url=jdbc:h2:mem:upload-new-teas;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;"
    + "INIT=CREATE SCHEMA IF NOT EXISTS myteacollection")
@ExtendWith(OutputCaptureExtension.class)
class UploadNewTeasIT {

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private TastingNoteRepository tastingNoteRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UploadTastingNotes uploadTastingNotes;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mvc;

  @Transactional
  @Test
  void noTeasInDb_uploadsAllTeas() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
    var teas = teaRepository.findAll();
    assertEquals(7, teas.size());
    var first = teas.get(0);
    assertEquals(UploadNewTeas.USER_EMAIL, first.getUser().getEmail());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", first.getName());
    assertEquals("meetea-doubleshot-2022", first.getSlug());
    assertEquals(Set.of("Dark Tea", "Shu Puerh"), getNames(first.getTypes()));
    assertEquals("Meetea", first.getVendor().getName());
    assertEquals(4.f, first.getPrice());
    assertEquals(2, first.getImages().size());
    assertEquals(Set.of("meetea-2025-jan", "meetea-2024-dec"), getLabels(first.getTags()));
    var second = teas.get(1);
    assertEquals(UploadNewTeas.USER_EMAIL, second.getUser().getEmail());
    assertEquals("https://meileaf.com/tea/luminary-misfit/", second.getUrl());
    assertEquals("mei-leaf-luminary-misfit-2022", second.getSlug());
    assertEquals(3, second.getImages().size());
    assertNull(second.getPrice());
    assertTrue(second.getTags().isEmpty());
    var third = teas.get(2);
    assertEquals(UploadNewTeas.USER_EMAIL, third.getUser().getEmail());
    assertEquals("2021 Zhenghe Shou Mei Blend", third.getName());
    assertEquals("mei-leaf-simple-dreams-2-2021", third.getSlug());
    assertEquals(3, third.getImages().size());
    assertEquals(7.29f, third.getPrice());
    var fourth = teas.get(3);
    assertEquals(UploadNewTeas.USER_EMAIL, fourth.getUser().getEmail());
    assertEquals("Shou Mei 2017", fourth.getTitle());
    assertEquals("meetea-shou-mei-2017", fourth.getSlug());
    assertEquals(2, fourth.getImages().size());
    assertEquals(4.2f, fourth.getPrice());
    var fifth = teas.get(4);
    assertEquals(UploadNewTeas.USER_EMAIL, fifth.getUser().getEmail());
    assertEquals("Jade Star 8", fifth.getTitle());
    assertEquals("mei-leaf-jade-star-8", fifth.getSlug());
    assertNull(fifth.getScope().getSeason());
    var sixth = teas.get(5);
    assertEquals("Approximate Season Fixture", sixth.getTitle());
    assertEquals("mei-leaf-approximate-season-fixture", sixth.getSlug());
    assertEquals("Early 2000s", sixth.getScope().getSeason());
    var seventh = teas.get(6);
    assertEquals("Long Description Fixture", seventh.getTitle());
    assertEquals("mei-leaf-long-description-fixture", seventh.getSlug());
    assertNull(seventh.getScope().getSeason());
  }

  @Test
  @Transactional
  void noImages_uploadsTeaAndRendersSharedPlaceholder(@TempDir Path rootDir) throws Exception {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Tea Without Images");
    Files.delete(rootDir.resolve("1").resolve("01.jpg"));
    createTeaFixture(rootDir, "2", "Tea With Image");

    uploadNewTeas.run(rootDir.toFile());

    assertEquals(2, teaRepository.count());
    var withoutImages = teaRepository.findBySlug("mei-leaf-tea-without-images-2024").orElseThrow();
    var withImage = teaRepository.findBySlug("mei-leaf-tea-with-image-2024").orElseThrow();
    assertTrue(withoutImages.getImages().isEmpty());
    assertEquals(1, withImage.getImages().size());
    assertEquals(1, countRows("TeaImages"));
    assertEquals(1, countRows("TeaImageData"));
    var imageId = withImage.getImages().stream().findFirst().orElseThrow().getId();

    for (var endpoint : List.of("/", "/index", "/filter", "/search")) {
      var gallery = mvc.perform(get(endpoint)
        .param("query", "Tea")
        .param("teaTypeId", "0")
        .param("vendorId", "0")
        .param("availabilityId", "0"))
        .andExpect(status().isOk());
      verifyImageSource(gallery, withoutImages.getTitle(), "/img/no-image.jpg");
      verifyImageSource(gallery, withImage.getTitle(), "/images/" + imageId);
      ITUtils.doesNotContainStrings(gallery, "/images/null");
    }

    var placeholderView = mvc.perform(get("/teas/" + withoutImages.getSlug()))
      .andExpect(status().isOk());
    verifyImageSource(placeholderView, withoutImages.getTitle(), "/img/no-image.jpg");
    ITUtils.containsStrings(placeholderView, "tea-gallery-image", "data-target=\"#imgModal\"");
    ITUtils.doesNotContainStrings(placeholderView, "/images/null");

    var imageView = mvc.perform(get("/teas/" + withImage.getSlug()))
      .andExpect(status().isOk());
    verifyImageSource(imageView, withImage.getTitle(), "/images/" + imageId);
    ITUtils.doesNotContainStrings(imageView, "/img/no-image.jpg");
  }

  @Test
  @Transactional
  void duplicateSlug_isLoggedAndAbortsBatch(CapturedOutput output, @TempDir Path rootDir) throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    createTeaFixture(rootDir, "1", "Duplicate Tea");
    createTeaFixture(rootDir, "2", "Duplicate Tea");
    createTeaFixture(rootDir, "3", "Unreached Tea");

    var ex = assertThrows(
      IllegalArgumentException.class, () -> uploadNewTeas.run(rootDir.toFile()));

    assertTrue(ex.getMessage().contains("mei-leaf-duplicate-tea-2024"));
    assertTrue(output.getAll().contains("Failed to upload tea #2"));
    var teas = teaRepository.findAll();
    assertEquals(1, teas.size());
    assertEquals("mei-leaf-duplicate-tea-2024", teas.get(0).getSlug());
  }

  @Test
  @Transactional
  void invalidTea_isRejectedLoggedAndAborts(CapturedOutput output, @TempDir Path rootDir) throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    var teaDir = Files.createDirectory(rootDir.resolve("1"));
    Files.writeString(teaDir.resolve(TeaRecord.INFO_FILE_NAME), """
      {
        "title": "",
        "name": "Invalid Tea",
        "description": "A tea with a blank title.",
        "types": [ "Dark Tea" ],
        "vendor": "Meetea",
        "url": "https://example.com/invalid",
        "origin": "Yunnan",
        "cultivar": "Da Ye Zhong",
        "season": "Spring 2024",
        "elevation": "1500m",
        "price": "4",
        "brewingInstructions": "95C",
        "inStock": true
      }
      """);

    var ex = assertThrows(
      IllegalArgumentException.class, () -> uploadNewTeas.run(rootDir.toFile()));

    assertTrue(ex.getMessage().toLowerCase().contains("title"));
    assertTrue(output.getAll().contains("Failed to upload tea #1"),
      "expected the rejected tea to be reported via the logger");
    assertEquals(0, teaRepository.count());
  }

  @Test
  @DirtiesContext
  void reset_reloadsEditedTeasAndImages_restartsTeaIdsAndClearsNotes(@TempDir Path rootDir)
    throws IOException {

    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Original Tea");
    createTeaFixture(rootDir, "2", "Removed Tea");
    uploadNewTeas.run(rootDir.toFile());
    var original = teaRepository.findAll().get(0);
    var owner = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow();
    tastingNoteRepository.saveAndFlush(
      new TastingNoteEntity(original, owner, 8, LocalDate.of(2026, 1, 1), "Old note."));
    var userCount = countRows("Users");
    var vendorCount = countRows("Vendors");
    var typeCount = countRows("TeaTypes");
    var tagCount = countRows("Tags");

    createTeaFixture(rootDir, "1", "Corrected Tea");
    Files.delete(rootDir.resolve("1").resolve("01.jpg"));
    var replacement = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(replacement, "png", rootDir.resolve("1").resolve("01.png").toFile());
    Files.copy(toFile("teas/02/01.jpg").toPath(), rootDir.resolve("1").resolve("02.jpg"));
    Files.writeString(rootDir.resolve("1").resolve("tasting-notes.json"), """
      [ { "rating": 4.5, "date": "2026-01-02", "body": ["Restored separately."] } ]
      """);
    Files.delete(rootDir.resolve("2").resolve(TeaRecord.INFO_FILE_NAME));
    Files.delete(rootDir.resolve("2").resolve("01.jpg"));
    Files.delete(rootDir.resolve("2"));

    uploadNewTeas.run(rootDir.toFile());
    assertEquals(2, teaRepository.count());
    assertEquals("Original Tea", teaRepository.findById(original.getId()).orElseThrow().getTitle());
    assertEquals(1, tastingNoteRepository.count());

    uploadNewTeas.run(rootDir.toFile(), true);

    var teas = teaRepository.findAll();
    assertEquals(1, teas.size());
    var reloaded = teas.get(0);
    assertEquals(1L, reloaded.getId());
    assertEquals(owner.getId(), reloaded.getUser().getId());
    assertEquals("Corrected Tea", reloaded.getTitle());
    assertEquals("mei-leaf-corrected-tea-2024", reloaded.getSlug());
    assertEquals(Set.of("Dark Tea"), getNames(reloaded.getTypes()));
    assertEquals(Set.of("meetea-2025-jan"), getLabels(reloaded.getTags()));
    assertEquals(2, reloaded.getImages().size());
    assertNull(reloaded.getAverageRatingHalfStars());
    assertEquals(0, tastingNoteRepository.count());
    assertEquals(2, countRows("TeaImages"));
    assertEquals(2, countRows("TeaImageData"));
    assertEquals(1, countRows("Teas_TeaTypes"));
    assertEquals(1, countRows("Teas_Tags"));
    var replacementBytes = new JpgCompression(replacement).getBytes();
    var storedImages = jdbcTemplate.queryForList(
      "SELECT bytes FROM myteacollection.TeaImageData", byte[].class);
    assertTrue(storedImages.stream().anyMatch(bytes -> Arrays.equals(replacementBytes, bytes)));
    assertEquals(userCount, countRows("Users"));
    assertEquals(vendorCount, countRows("Vendors"));
    assertEquals(typeCount, countRows("TeaTypes"));
    assertEquals(tagCount, countRows("Tags"));

    uploadTastingNotes.run(rootDir.toFile());
    assertEquals("Restored separately.",
      tastingNoteRepository.findByTeaIdNewestFirst(1L).get(0).getBody());
    createTeaFixture(rootDir, "2", "New Tea");
    uploadNewTeas.run(rootDir.toFile());
    assertEquals(2, teaRepository.count());
    assertEquals("New Tea", teaRepository.findById(2L).orElseThrow().getTitle());
    assertEquals(1, tastingNoteRepository.count());
  }

  @Test
  @Transactional
  void reset_missingSourceDirectory_doesNotDeleteExistingTeas(@TempDir Path rootDir) throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    uploadNewTeas.run(toFile("teas"));
    var originalIds = teaRepository.findAll().stream().map(tea -> tea.getId()).toList();
    var missingDir = rootDir.resolve("missing").toFile();

    var ex = assertThrows(IllegalArgumentException.class,
      () -> uploadNewTeas.run(missingDir, true));

    assertEquals("Root directory does not exist or is not a directory: " + missingDir, ex.getMessage());
    assertEquals(originalIds, teaRepository.findAll().stream().map(tea -> tea.getId()).toList());
  }

  @Test
  @DirtiesContext
  void reset_emptySource_clearsDynamicTablesAndRestartsTeaIds(@TempDir Path rootDir)
    throws IOException {

    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Original Tea");
    uploadNewTeas.run(rootDir.toFile());
    var original = teaRepository.findAll().get(0);
    var owner = userRepository.findByEmailIgnoreCase(UploadNewTeas.USER_EMAIL).orElseThrow();
    tastingNoteRepository.saveAndFlush(
      new TastingNoteEntity(original, owner, 8, LocalDate.of(2026, 1, 1), "Old note."));
    var emptyDir = Files.createDirectory(rootDir.resolve("empty"));

    uploadNewTeas.run(emptyDir.toFile(), true);

    assertEquals(0, teaRepository.count());
    assertEquals(0, tastingNoteRepository.count());
    assertEquals(0, countRows("TeaImages"));
    assertEquals(0, countRows("TeaImageData"));
    assertEquals(0, countRows("Teas_TeaTypes"));
    assertEquals(0, countRows("Teas_Tags"));
    assertEquals(1, countRows("Users"));

    createTeaFixture(emptyDir, "1", "New Tea");
    uploadNewTeas.run(emptyDir.toFile());
    assertEquals("New Tea", teaRepository.findById(1L).orElseThrow().getTitle());
  }

  @Test
  @DirtiesContext
  void reset_malformedJson_abortsAfterClearingExistingTeas(@TempDir Path rootDir) throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Original Tea");
    uploadNewTeas.run(rootDir.toFile());
    Files.writeString(rootDir.resolve("1").resolve(TeaRecord.INFO_FILE_NAME), "{ invalid json");

    assertThrows(CannotLoadTeaInfoException.class,
      () -> uploadNewTeas.run(rootDir.toFile(), true));

    assertEquals(0, teaRepository.count());
    assertEquals(0, countRows("TeaImages"));
    assertEquals(0, countRows("TeaImageData"));
    assertEquals(1, countRows("Users"));
  }

  @Test
  @DirtiesContext
  void reset_missingImportUser_abortsAfterClearingExistingTeas(@TempDir Path rootDir) throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Original Tea");
    uploadNewTeas.run(rootDir.toFile());
    jdbcTemplate.update("UPDATE myteacollection.Users SET email = ? WHERE email = ?",
      "another-owner@example.com", UploadNewTeas.USER_EMAIL);

    var ex = assertThrows(IllegalStateException.class,
      () -> uploadNewTeas.run(rootDir.toFile(), true));

    assertTrue(ex.getMessage().contains("User not found"));
    assertEquals(0, teaRepository.count());
    assertEquals(0, countRows("TeaImages"));
    assertEquals(0, countRows("TeaImageData"));
    assertEquals(1, countRows("Users"));
  }

  @Test
  @DirtiesContext
  void reset_rejectedTeaKeepsCommittedPrefix_incrementalRunResumes(@TempDir Path rootDir)
    throws IOException {

    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Test", "Owner");
    createTeaFixture(rootDir, "1", "Original Tea");
    createTeaFixture(rootDir, "2", "Second Tea");
    uploadNewTeas.run(rootDir.toFile());
    createTeaFixture(rootDir, "1", "Corrected Tea");
    createTeaFixture(rootDir, "2", "");

    var ex = assertThrows(IllegalArgumentException.class,
      () -> uploadNewTeas.run(rootDir.toFile(), true));

    assertTrue(ex.getMessage().toLowerCase().contains("title"));
    assertEquals(1, teaRepository.count());
    assertEquals("Corrected Tea", teaRepository.findById(1L).orElseThrow().getTitle());

    createTeaFixture(rootDir, "2", "Second Tea");
    uploadNewTeas.run(rootDir.toFile());

    assertEquals(2, teaRepository.count());
    assertEquals("Corrected Tea", teaRepository.findById(1L).orElseThrow().getTitle());
    assertEquals("Second Tea", teaRepository.findById(2L).orElseThrow().getTitle());
  }

  private void verifyImageSource(ResultActions actions, String title, String source) throws Exception {
    var html = actions.andReturn().getResponse().getContentAsString();
    var image = Pattern.compile("<img\\b[^>]*\\balt=\"" + Pattern.quote(title) + "\"[^>]*>")
      .matcher(html);
    assertTrue(image.find(), "Missing image for tea: " + title);
    assertTrue(image.group().contains("src=\"" + source + "\""), image.group());
  }

  private long countRows(String table) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM myteacollection." + table, Long.class);
  }

  private void createTeaFixture(Path rootDir, String id, String title) throws IOException {
    var teaDir = Files.createDirectories(rootDir.resolve(id));
    Files.writeString(teaDir.resolve(TeaRecord.INFO_FILE_NAME), """
      {
        "title": "%s",
        "name": "",
        "description": "A valid test tea.",
        "types": [ "Dark Tea" ],
        "vendor": "Mei Leaf",
        "url": "https://example.com/tea",
        "origin": "Yunnan",
        "cultivar": "Da Ye Zhong",
        "season": "Spring 2024",
        "elevation": "1500m",
        "price": "4",
        "brewingInstructions": "95C",
        "inStock": true,
        "tags": [ "meetea-2025-jan" ]
      }
      """.formatted(title));
    Files.copy(toFile("teas/01/01.jpg").toPath(), teaDir.resolve("01.jpg"), REPLACE_EXISTING);
  }

  private Set<String> getLabels(Set<TagEntity> tags) {
    return tags.stream().map(TagEntity::getLabel).collect(toSet());
  }

  private Set<String> getNames(Set<TeaTypeEntity> types) {
    return types.stream().map(TeaTypeEntity::getName).collect(toSet());
  }
}
