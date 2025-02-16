package cz.dusanrychnovsky.myteacollection.util.upload;

import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;

@DataJpaTest()
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace= NONE)
public class UploadNewTeasTests {

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private TeaRepository teaRepository;

  @Test
  public void noTeasInDb_uploadsAllTeas() throws IOException {
    uploadNewTeas.run();
    var teas = teaRepository.findAll();
    assertEquals(2, teas.size());
    var first = teas.get(0);
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", first.getName());
    assertEquals(Set.of("Dark", "Shu Puerh"), getNames(first.getTypes()));
    assertEquals("Meetea", first.getVendor().getName());
    assertEquals(2, first.getImages().size());
    var second = teas.get(1);
    assertEquals("https://meileaf.com/tea/luminary-misfit/", second.getUrl());
    assertEquals(3, second.getImages().size());
  }

  private Set<String> getNames(Set<TeaType> types) {
    return types.stream().map(TeaType::getName).collect(toSet());
  }
}
