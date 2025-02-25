package cz.dusanrychnovsky.myteacollection.util.upload;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;

@DataJpaTest()
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace= NONE)
public class UploadNewTeasIntegrationTests {

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private TeaRepository teaRepository;

  @Test
  public void noTeasInDb_uploadsAllTeas() throws IOException {
    uploadNewTeas.run(toFile("teas"));
    var teas = teaRepository.findAll();
    assertEquals(2, teas.size());
    var first = teas.get(0);
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", first.getName());
    assertEquals(Set.of("Dark Tea", "Shu Puerh"), getNames(first.getTypes()));
    assertEquals("Meetea", first.getVendor().getName());
    assertEquals(2, first.getImages().size());
    assertEquals(Set.of("meetea-2025-jan", "meetea-2024-dec"), getLabels(first.getTags()));
    var second = teas.get(1);
    assertEquals("https://meileaf.com/tea/luminary-misfit/", second.getUrl());
    assertEquals(3, second.getImages().size());
    assertTrue(second.getTags().isEmpty());
  }

  private Set<String> getLabels(Set<TagEntity> tags) {
    return tags.stream().map(TagEntity::getLabel).collect(toSet());
  }

  private Set<String> getNames(Set<TeaTypeEntity> types) {
    return types.stream().map(TeaTypeEntity::getName).collect(toSet());
  }
}
