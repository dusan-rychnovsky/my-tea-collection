package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.util.upload.UpdateTeasAvailability;
import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
class UpdateTeasAvailabilityIT {

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UploadNewTeas uploadNewTeas;

  @Autowired
  private UpdateTeasAvailability updateTeasAvailability;

  @Autowired
  private TeaRepository teaRepository;

  @Transactional
  @Test
  void updatesOnlyTeasWithDivergingAvailability() throws IOException {
    createUser.run(UploadNewTeas.USER_EMAIL, "pwd", "Dušan", "Rychnovský");
    uploadNewTeas.run(toFile("teas"));

    var teas = teaRepository.findAll();
    teaRepository.updateInStock(teas.get(0).getId(), false);
    teaRepository.updateInStock(teas.get(2).getId(), false);

    updateTeasAvailability.run(toFile("teas"));

    assertTrue(teaRepository.findAll().stream().allMatch(TeaEntity::isInStock));
  }
}
