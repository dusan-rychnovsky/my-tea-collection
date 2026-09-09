package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.tea.ingest.TeaRecord;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ExtendWith(OutputCaptureExtension.class)
class UploadNewTeasIT {

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private TeaRepository teaRepository;

  @Transactional
  @Test
  void noTeasInDb_uploadsAllTeas() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));
    var teas = teaRepository.findAll();
    assertEquals(5, teas.size());
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
        "title": "Invalid Tea",
        "name": "No Images Tea",
        "description": "A tea with no images.",
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

    assertTrue(ex.getMessage().toLowerCase().contains("image"));
    assertTrue(output.getAll().contains("Failed to upload tea #1"),
      "expected the rejected tea to be reported via the logger");
    assertEquals(0, teaRepository.count());
  }

  private void createTeaFixture(Path rootDir, String id, String title) throws IOException {
    var teaDir = Files.createDirectory(rootDir.resolve(id));
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
        "inStock": true
      }
      """.formatted(title));
    Files.copy(toFile("teas/01/01.jpg").toPath(), teaDir.resolve("01.jpg"));
  }

  private Set<String> getLabels(Set<TagEntity> tags) {
    return tags.stream().map(TagEntity::getLabel).collect(toSet());
  }

  private Set<String> getNames(Set<TeaTypeEntity> types) {
    return types.stream().map(TeaTypeEntity::getName).collect(toSet());
  }
}
