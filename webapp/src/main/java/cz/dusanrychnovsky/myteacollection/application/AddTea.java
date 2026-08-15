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
 * adapters. Builds the domain {@link Tea} (which enforces its own invariants), validates that the
 * referenced vendor / types / tags and the owner exist, maps the tea to a persistence entity and
 * saves it, returning the new tea's id.
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
    var tea = new Tea(
      command.title(),
      command.name(),
      command.description(),
      command.url(),
      command.scope(),
      command.price(),
      command.brewingInstructions(),
      command.inStock(),
      command.vendorId(),
      command.typeIds(),
      command.tagIds(),
      command.images());

    var user = userRepository.findById(command.userId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + command.userId()));

    var vendor = vendorRepository.findById(tea.getVendorId())
      .orElseThrow(() -> new IllegalArgumentException("Invalid vendor ID: " + tea.getVendorId()));

    var types = new HashSet<>(teaTypeRepository.findAllById(tea.getTypeIds()));
    if (types.size() != tea.getTypeIds().size()) {
      throw new IllegalArgumentException("One or more tea type IDs are invalid: " + tea.getTypeIds());
    }

    var tags = new HashSet<>(tagRepository.findAllById(tea.getTagIds()));
    if (tags.size() != tea.getTagIds().size()) {
      throw new IllegalArgumentException("One or more tag IDs are invalid: " + tea.getTagIds());
    }

    return teaRepository.save(TeaMapper.toEntity(tea, user, vendor, types, tags)).getId();
  }
}
