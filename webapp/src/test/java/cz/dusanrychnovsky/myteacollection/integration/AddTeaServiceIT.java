package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.application.AddTea;
import cz.dusanrychnovsky.myteacollection.application.AddTeaCommand;
import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;
import cz.dusanrychnovsky.myteacollection.util.users.CreateUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    return new AddTeaCommand(
      "Title",
      "Name",
      "Description",
      "https://example.com/tea",
      new TeaScope("Spring 2024", "Da Ye Zhong", "Yunnan", "1500m"),
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
  void handle_validCommand_persistsTeaWithImagesAndReturnsId() {
    var id = addTea.handle(command(
      userId, 1L, Set.of(25L), Set.of(1L), new Price(7.29f), List.of(new byte[]{1, 2}, new byte[]{3})));

    var entity = teaRepository.findById(id).orElseThrow();
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
  void handle_invalidUserId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(999_999L, 1L, Set.of(25L), Set.of(), null, List.of())));
    assertTrue(ex.getMessage().contains("user"));
  }

  @Test
  @Transactional
  void handle_invalidVendorId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 999L, Set.of(25L), Set.of(), null, List.of())));
    assertTrue(ex.getMessage().contains("vendor"));
  }

  @Test
  @Transactional
  void handle_invalidTypeId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(999L), Set.of(), null, List.of())));
    assertTrue(ex.getMessage().contains("type"));
  }

  @Test
  @Transactional
  void handle_invalidTagId_throws() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> addTea.handle(command(userId, 1L, Set.of(25L), Set.of(999L), null, List.of())));
    assertTrue(ex.getMessage().contains("tag"));
  }
}
