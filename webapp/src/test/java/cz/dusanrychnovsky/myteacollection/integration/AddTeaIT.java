package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static cz.dusanrychnovsky.myteacollection.ingest.TeaRecord.loadFrom;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class AddTeaIT {

  private static final String TEST_USER_EMAIL = "testuser@example.com";
  private static final String TEST_USER_ROLE = "USER";

  @Autowired
  private MockMvc mvc;

  @Autowired
  private CreateUser createUser;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private VendorRepository vendorRepository;

  @Autowired
  private TeaTypeRepository teaTypeRepository;

  @BeforeEach
  void setup() throws IOException {
    createUser.run(TEST_USER_EMAIL, "pwd", "Dušan", "Rychnovský");
  }

  @Test
  @Transactional
  void teaAdd_displaysTeaAddForm() throws Exception {
    mvc.perform(get("/teas/add")
      .with(user(TEST_USER_EMAIL).roles(TEST_USER_ROLE)))
      .andExpect(status().isOk())
      .andExpect(view().name("tea-add"));
  }

  @Test
  @Transactional
  void addTea_validSubmission_insertsNewTeaAndRedirectsToTeaView() throws Exception {
    var vendors = vendorRepository.findAll();
    var teaTypes = teaTypeRepository.findAll();

    var tea = loadFrom(toFile("teas/01"));
    mvc.perform(post("/teas/add")
      .with(user(TEST_USER_EMAIL).roles(TEST_USER_ROLE))
      .with(csrf())
      .param("url", tea.getUrl())
      .param("name", tea.getName())
      .param("title", tea.getTitle())
      .param("description", tea.getDescription())
      .param("vendorId", toVendorId(tea.getVendor(), vendors))
      .param("teaTypes", toTeaTypeIds(tea.getTypes(), teaTypes))
      .param("origin", tea.getOrigin())
      .param("cultivar", tea.getCultivar())
      .param("season", tea.getSeason())
      .param("elevation", tea.getElevation())
      .param("price", tea.getPrice())
      .param("brewingInstructions", tea.getBrewingInstructions()))
      .andExpect(status().is3xxRedirection());

    var teaEntity = teaRepository.findAll().stream()
      .filter(entity -> tea.getName().equals(entity.getName()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB"));

    assertEquals(tea.getUrl(), teaEntity.getUrl());
    assertEquals(tea.getName(), teaEntity.getName());
    assertEquals(tea.getTitle(), teaEntity.getTitle());
    assertEquals(tea.getDescription(), teaEntity.getDescription());

    assertEquals(tea.getVendor(), teaEntity.getVendor().getName());
    assertEquals(
      tea.getTypes(),
      teaEntity.getTypes().stream()
        .map(TeaTypeEntity::getName)
        .collect(toSet())
    );

    assertEquals(tea.getOrigin(), teaEntity.getScope().getOrigin());
    assertEquals(tea.getCultivar(), teaEntity.getScope().getCultivar());
    assertEquals(tea.getSeason(), teaEntity.getScope().getSeason());
    assertEquals(tea.getElevation(), teaEntity.getScope().getElevation());

    assertEquals(Price.parse(tea.getPrice()).map(Price::amountPerGram).orElse(null), teaEntity.getPrice());

    assertEquals(tea.getBrewingInstructions(), teaEntity.getBrewingInstructions());
    assertTrue(teaEntity.isInStock());
  }

  @Test
  @Transactional
  void addTea_withImages_persistsImages() throws Exception {
    var vendors = vendorRepository.findAll();
    var teaTypes = teaTypeRepository.findAll();
    var tea = loadFrom(toFile("teas/01"));

    var firstImage = new MockMultipartFile(
      "images", "01.jpg", "image/jpeg", Files.readAllBytes(toFile("teas/01/01.jpg").toPath()));
    var secondImage = new MockMultipartFile(
      "images", "02.jpg", "image/jpeg", Files.readAllBytes(toFile("teas/01/02.jpg").toPath()));

    mvc.perform(multipart("/teas/add")
      .file(firstImage)
      .file(secondImage)
      .with(user(TEST_USER_EMAIL).roles(TEST_USER_ROLE))
      .with(csrf())
      .param("url", tea.getUrl())
      .param("name", tea.getName())
      .param("title", tea.getTitle())
      .param("description", tea.getDescription())
      .param("vendorId", toVendorId(tea.getVendor(), vendors))
      .param("teaTypes", toTeaTypeIds(tea.getTypes(), teaTypes)))
      .andExpect(status().is3xxRedirection());

    var teaEntity = teaRepository.findAll().stream()
      .filter(entity -> tea.getName().equals(entity.getName()))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("Tea not found in DB"));

    assertEquals(2, teaEntity.getImages().size());
  }

  @Test
  @Transactional
  void addTea_negativePrice_isRejected() {
    var tea = loadFrom(toFile("teas/01"));
    var ex = assertThrows(Exception.class, () -> mvc.perform(post("/teas/add")
      .with(user(TEST_USER_EMAIL).roles(TEST_USER_ROLE))
      .with(csrf())
      .param("url", tea.getUrl())
      .param("name", tea.getName())
      .param("title", tea.getTitle())
      .param("description", tea.getDescription())
      .param("vendorId", toVendorId(tea.getVendor(), vendorRepository.findAll()))
      .param("teaTypes", toTeaTypeIds(tea.getTypes(), teaTypeRepository.findAll()))
      .param("price", "-5")));

    assertInstanceOf(IllegalArgumentException.class, NestedExceptionUtils.getMostSpecificCause(ex));
  }

  private String[] toTeaTypeIds(Set<String> teaTypeNames, List<TeaTypeEntity> entities) {
    return entities.stream()
      .filter(type -> teaTypeNames.contains(type.getName()))
      .map(type -> String.valueOf(type.getId()))
      .toArray(String[]::new);
  }

  private String toVendorId(String vendorName, List<VendorEntity> vendors) {
    return vendors.stream()
      .filter(vendor -> vendor.getName().equals(vendorName))
      .map(vendor -> String.valueOf(vendor.getId()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + vendorName));
  }
}
