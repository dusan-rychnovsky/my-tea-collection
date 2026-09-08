package cz.dusanrychnovsky.myteacollection.tea.application;

import cz.dusanrychnovsky.myteacollection.persistence.TagRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeRepository;
import cz.dusanrychnovsky.myteacollection.persistence.VendorRepository;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.domain.Tea;
import cz.dusanrychnovsky.myteacollection.domain.TeaSlug;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Application service for the "add a tea" use case, shared by the web and ingest inbound
 * adapters. Builds the domain {@link Tea} (which enforces its own invariants), validates that the
 * referenced vendor / types / tags and the owner exist, maps the tea to a persistence entity and
 * saves it, returning the new tea's id and slug.
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
  public AddedTea handle(AddTeaCommand command) {
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

    var slug = TeaSlug.from(tea, vendor.getName());
    if (teaRepository.existsBySlug(slug.value())) {
      throw new IllegalArgumentException("A tea with slug '" + slug.value() + "' already exists.");
    }

    var types = new HashSet<>(teaTypeRepository.findAllById(tea.getTypeIds()));
    if (types.size() != tea.getTypeIds().size()) {
      throw new IllegalArgumentException("One or more tea type IDs are invalid: " + tea.getTypeIds());
    }

    var tags = new HashSet<>(tagRepository.findAllById(tea.getTagIds()));
    if (tags.size() != tea.getTagIds().size()) {
      throw new IllegalArgumentException("One or more tag IDs are invalid: " + tea.getTagIds());
    }

    var saved = teaRepository.save(TeaMapper.toEntity(tea, slug, user, vendor, types, tags));
    return new AddedTea(saved.getId(), slug);
  }
}
