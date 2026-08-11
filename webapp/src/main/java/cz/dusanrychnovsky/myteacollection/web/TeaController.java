package cz.dusanrychnovsky.myteacollection.web;

import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TagRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageDataEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaImageRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaScope;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeRepository;
import cz.dusanrychnovsky.myteacollection.db.VendorRepository;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.util.upload.JpgCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

@Controller
public class TeaController {

  private static final Logger logger = LoggerFactory.getLogger(TeaController.class);

  private final UserRepository userRepository;
  private final VendorRepository vendorRepository;
  private final TeaTypeRepository teaTypeRepository;
  private final TeaImageRepository teaImageRepository;
  private final TeaRepository teaRepository;
  private final TagRepository tagRepository;

  @Autowired
  public TeaController(
    UserRepository userRepository,
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TeaImageRepository teaImageRepository,
    TeaRepository teaRepository,
    TagRepository tagRepository) {

    this.userRepository = userRepository;
    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.teaImageRepository = teaImageRepository;
    this.teaRepository = teaRepository;
    this.tagRepository = tagRepository;
  }

  @GetMapping("/teas/add")
  public String teaAdd(Model model) {
    model.addAttribute("vendors", vendorRepository.findAll());
    model.addAttribute("teaTypes", teaTypeRepository.findAll());
    model.addAttribute(
      "tags",
      tagRepository.findAll().stream()
        .sorted(comparing(TagEntity::getLabel))
    );
    return "tea-add";
  }

  @PostMapping("/teas/add")
  @Transactional
  public String addTea(
    Authentication authentication,
    @RequestParam String url,
    @RequestParam String title,
    @RequestParam String name,
    @RequestParam String description,
    @RequestParam(value = "teaTypes") List<Long> teaTypeIds,
    @RequestParam Long vendorId,
    @RequestParam(required = false) String season,
    @RequestParam(required = false) String origin,
    @RequestParam(required = false) String elevation,
    @RequestParam(required = false) String cultivar,
    @RequestParam(required = false) String brewingInstructions,
    @RequestParam(required = false) Float price,
    @RequestParam(value = "tags", required = false) List<Long> tagIds,
    @RequestParam(required = false) List<MultipartFile> images
  ) throws Exception {
    // TODO: form validation (mandatory fields, URL format, etc.)

    var emailAddress = authentication.getName();
    var user = userRepository.findByEmailIgnoreCase(emailAddress)
      .orElseThrow(() -> new IllegalArgumentException("User not found with email address: " + emailAddress));

    var vendorEntity = vendorRepository.findById(vendorId)
      .orElseThrow(() -> new IllegalArgumentException("Invalid vendor ID: " + vendorId));

    var teaTypeEntities = new HashSet<>(teaTypeRepository.findAllById(teaTypeIds));
    if (teaTypeEntities.size() != teaTypeIds.size()) {
      throw new IllegalArgumentException("One or more tea type IDs are invalid: " + teaTypeIds);
    }

    tagIds = tagIds != null ? tagIds : emptyList();
    var tagEntities = new HashSet<>(tagRepository.findAllById(tagIds));
    if (tagEntities.size() != tagIds.size()) {
      throw new IllegalArgumentException("One or more tag IDs are invalid: " + tagIds);
    }

    var teaEntity = new TeaEntity(
      user,
      vendorEntity,
      teaTypeEntities,
      title,
      name,
      description,
      url,
      new TeaScope(
        season,
        cultivar,
        origin,
        elevation
      ),
      price,
      brewingInstructions,
      true,
      tagEntities
    );

    teaEntity = teaRepository.save(teaEntity);

    if (images != null) {
      var idx = 0;
      for (var imgFile : images) {
        if (!imgFile.isEmpty()) {
          idx++;
          var bytes = getCompressedBytes(imgFile);
          var imgEntity = new TeaImageEntity(
            teaEntity,
            idx,
            new TeaImageDataEntity(bytes)
          );
          teaImageRepository.save(imgEntity);
        }
      }
    }

    return "redirect:/teas/" + teaEntity.getId();
  }

  private byte[] getCompressedBytes(MultipartFile imgFile) throws IOException {
    var image = ImageIO.read(imgFile.getInputStream());
    var origLen = imgFile.getBytes().length;
    var compressedBytes = new JpgCompression(image).getBytes();
    var compressedLen = compressedBytes.length;
    logger.info("JPG compression: original size {}, compressed size {}, ratio {}",
      origLen, compressedLen, (float) compressedLen / origLen);
    return compressedBytes;
  }
}
