package cz.dusanrychnovsky.myteacollection.application;

import cz.dusanrychnovsky.myteacollection.db.TagRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeRepository;
import cz.dusanrychnovsky.myteacollection.db.VendorRepository;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.domain.Tea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Application service for the "add a tea" use case, shared by the web and ingest inbound
 * adapters. Resolves and validates the command's reference ids, builds the domain {@link Tea},
 * maps it to a persistence entity and saves it, returning the new tea's id.
 */
@Service
public class AddTea {

  private final TeaRepository teaRepository;
  private final UserRepository userRepository;
  private final VendorRepository vendorRepository;
  private final TeaTypeRepository teaTypeRepository;
  private final TagRepository tagRepository;

  @Autowired
  public AddTea(
    TeaRepository teaRepository,
    UserRepository userRepository,
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TagRepository tagRepository) {

    this.teaRepository = teaRepository;
    this.userRepository = userRepository;
    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.tagRepository = tagRepository;
  }

  @Transactional
  public Long handle(AddTeaCommand command) {
    var user = userRepository.findById(command.userId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + command.userId()));

    var vendor = vendorRepository.findById(command.vendorId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid vendor ID: " + command.vendorId()));

    var types = new HashSet<>(teaTypeRepository.findAllById(command.typeIds()));
    if (types.size() != command.typeIds().size()) {
      throw new IllegalArgumentException("One or more tea type IDs are invalid: " + command.typeIds());
    }

    var tags = new HashSet<>(tagRepository.findAllById(command.tagIds()));
    if (tags.size() != command.tagIds().size()) {
      throw new IllegalArgumentException("One or more tag IDs are invalid: " + command.tagIds());
    }

    var tea = new Tea(
      command.title(),
      command.name(),
      command.description(),
      command.url(),
      command.scope(),
      command.price(),
      command.brewingInstructions(),
      command.inStock(),
      command.userId(),
      command.vendorId(),
      command.typeIds(),
      command.tagIds(),
      command.images());

    return teaRepository.save(TeaMapper.toEntity(tea, user, vendor, types, tags)).getId();
  }
}
