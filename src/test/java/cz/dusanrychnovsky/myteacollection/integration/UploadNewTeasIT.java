package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest()
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
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
    assertEquals(4, teas.size());
    var first = teas.get(0);
    assertEquals(UploadNewTeas.USER_EMAIL, first.getUser().getEmail());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", first.getName());
    assertEquals(Set.of("Dark Tea", "Shu Puerh"), getNames(first.getTypes()));
    assertEquals("Meetea", first.getVendor().getName());
    assertEquals(4.f, first.getPrice());
    assertEquals(2, first.getImages().size());
    assertEquals(Set.of("meetea-2025-jan", "meetea-2024-dec"), getLabels(first.getTags()));
    var second = teas.get(1);
    assertEquals(UploadNewTeas.USER_EMAIL, second.getUser().getEmail());
    assertEquals("https://meileaf.com/tea/luminary-misfit/", second.getUrl());
    assertEquals(3, second.getImages().size());
    assertNull(second.getPrice());
    assertTrue(second.getTags().isEmpty());
    var third = teas.get(2);
    assertEquals(UploadNewTeas.USER_EMAIL, third.getUser().getEmail());
    assertEquals("2021 Zhenghe Shou Mei Blend", third.getName());
    assertEquals(3, third.getImages().size());
    assertEquals(7.29f, third.getPrice());
    var fourth = teas.get(3);
    assertEquals(UploadNewTeas.USER_EMAIL, fourth.getUser().getEmail());
    assertEquals("Shou Mei 2017", fourth.getTitle());
    assertEquals(2, fourth.getImages().size());
    assertEquals(4.2f, fourth.getPrice());
  }

  private Set<String> getLabels(Set<TagEntity> tags) {
    return tags.stream().map(TagEntity::getLabel).collect(toSet());
  }

  private Set<String> getNames(Set<TeaTypeEntity> types) {
    return types.stream().map(TeaTypeEntity::getName).collect(toSet());
  }
}
