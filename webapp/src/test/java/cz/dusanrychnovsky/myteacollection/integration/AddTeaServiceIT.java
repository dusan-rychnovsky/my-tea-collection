package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.tea.application.AddTea;
import cz.dusanrychnovsky.myteacollection.tea.application.AddTeaCommand;
import cz.dusanrychnovsky.myteacollection.persistence.TagEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaScopeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AddTeaServiceIT {

  private static final String EMAIL = "addtea-service@example.com";

  @Autowired
  private AddTea addTea;

  @Autowired
  private CreateUser createUser;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TeaRepository teaRepository;

  private Long userId;

  @BeforeEach
  void setup() {
    createUser.run(EMAIL, "pwd", "Dušan", "Rychnovský");
    userId = userRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().getId();
  }

  private AddTeaCommand command(
    Long userId, Long vendorId, Set<Long> typeIds, Set<Long> tagIds, Price price, List<byte[]> images) {
    return command(
      "Title", "Spring 2024", userId, vendorId, typeIds, tagIds, price, images);
  }

  private AddTeaCommand command(
    String title, String season, Long userId, Long vendorId, Set<Long> typeIds,
    Set<Long> tagIds, Price price, List<byte[]> images) {
    return new AddTeaCommand(
      title,
      "Name",
      "Description",
      "https://example.com/tea",
      new TeaScope(season, "Da Ye Zhong", "Yunnan", "1500m"),
      price,
      "95°C",
      true,
      userId,
      vendorId,
      typeIds,
      tagIds,
      images);
  }

  @Test
  @Transactional
  void handle_validCommand_persistsTeaWithSlugAndImagesAndReturnsResult() {
    var result = addTea.handle(command(
      userId, 1L, Set.of(25L), Set.of(1L), new Price(7.29f), List.of(new byte[]{1, 2}, new byte[]{3})));

    assertEquals("mei-leaf-title-2024", result.slug().value());
    var entity = teaRepository.findById(result.id()).orElseThrow();
    assertEquals(entity, teaRepository.findBySlug(result.slug().value()).orElseThrow());
    assertEquals("mei-leaf-title-2024", entity.getSlug());
    assertEquals("Title", entity.getTitle());
    assertEquals(userId, entity.getUser().getId());
    assertEquals(1L, entity.getVendor().getId());
    assertEquals(Set.of(25L), entity.getTypes().stream().map(TeaTypeEntity::getId).collect(toSet()));
    assertEquals(Set.of(1L), entity.getTags().stream().map(TagEntity::getId).collect(toSet()));
    assertEquals(7.29f, entity.getPrice());
    assertEquals(2, entity.getImages().size());
  }

  @Test
  @Transactional
  void handle_titleAndSeasonYearsDiffer_rejectsTea() {
    var ex = assertThrows(IllegalArgumentException.class, () -> addTea.handle(command(
      "Tea 2023", "Spring 2024", userId, 1L, Set.of(25L), Set.of(), null, List.of(new byte[]{1}))));

    assertTrue(ex.getMessage().contains("title year 2023"));
    assertTrue(ex.getMessage().contains("season year 2024"));
    assertEquals(0, teaRepository.count());
  }

  @Test
  @Transactional
  void handle_duplicateSlug_rejectsSecondTea() {
    var command = command(
      userId, 1L, Set.of(25L), Set.of(), null, List.of(new byte[]{1}));
    addTea.handle(command);

    var ex = assertThrows(IllegalArgumentException.class, () -> addTea.handle(command));

    assertTrue(ex.getMessage().contains("mei-leaf-title-2024"));
    assertEquals(1, teaRepository.count());
  }

  @Test
  @Transactional
  void save_duplicateSlug_databaseRejectsSecondTea() {
    var result = addTea.handle(command(
      userId, 1L, Set.of(25L), Set.of(), null, List.of(new byte[]{1})));
    var existing = teaRepository.findById(result.id()).orElseThrow();

    assertThrows(
      DataIntegrityViolationException.class,
      () -> teaRepository.saveAndFlush(copyWithSlug(existing, existing.getSlug())));
  }

  @Test
  @Transactional
  void save_nullSlug_databaseRejectsTea() {
    var result = addTea.handle(command(
      userId, 1L, Set.of(25L), Set.of(), null, List.of(new byte[]{1})));
    var existing = teaRepository.findById(result.id()).orElseThrow();

    assertThrows(
      DataIntegrityViolationException.class,
      () -> teaRepository.saveAndFlush(copyWithSlug(existing, null)));
  }

  @Test
  @Transactional
  void handle_invalidUserId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(999_999L, 1L, Set.of(25L), Set.of(), null, List.of(new byte[]{1}))));
    assertTrue(ex.getMessage().contains("user"));
  }

  @Test
  @Transactional
  void handle_invalidVendorId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 999L, Set.of(25L), Set.of(), null, List.of(new byte[]{1}))));
    assertTrue(ex.getMessage().contains("vendor"));
  }

  @Test
  @Transactional
  void handle_invalidTypeId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(999L), Set.of(), null, List.of(new byte[]{1}))));
    assertTrue(ex.getMessage().contains("type"));
  }

  @Test
  @Transactional
  void handle_invalidTagId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(25L), Set.of(999L), null, List.of(new byte[]{1}))));
    assertTrue(ex.getMessage().contains("tag"));
  }

  @Test
  @Transactional
  void handle_noImages_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(25L), Set.of(), new Price(5f), List.of())));
    assertTrue(ex.getMessage().contains("image"));
  }

  @Test
  @Transactional
  void handle_noTypes_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(), Set.of(), new Price(5f), List.of(new byte[]{1}))));
    assertTrue(ex.getMessage().contains("type"));
  }

  private TeaEntity copyWithSlug(TeaEntity source, String slug) {
    var scope = source.getScope();
    return new TeaEntity(
      source.getUser(),
      source.getVendor(),
      source.getTypes(),
      slug,
      source.getTitle(),
      source.getName(),
      source.getDescription(),
      source.getUrl(),
      new TeaScopeEntity(scope.getSeason(), scope.getCultivar(), scope.getOrigin(), scope.getElevation()),
      source.getPrice(),
      source.getBrewingInstructions(),
      source.isInStock(),
      source.getTags());
  }
}
