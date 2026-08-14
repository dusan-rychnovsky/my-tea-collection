package cz.dusanrychnovsky.myteacollection.web;

import cz.dusanrychnovsky.myteacollection.application.AddTea;
import cz.dusanrychnovsky.myteacollection.application.AddTeaCommand;
import cz.dusanrychnovsky.myteacollection.db.TagEntity;
import cz.dusanrychnovsky.myteacollection.db.TagRepository;
import cz.dusanrychnovsky.myteacollection.db.TeaTypeRepository;
import cz.dusanrychnovsky.myteacollection.db.VendorRepository;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.domain.Price;
import cz.dusanrychnovsky.myteacollection.domain.TeaScope;
import cz.dusanrychnovsky.myteacollection.util.JpgCompression;
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

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.ArrayList;
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
  private final TagRepository tagRepository;
  private final AddTea addTea;

  @Autowired
  public TeaController(
    UserRepository userRepository,
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TagRepository tagRepository,
    AddTea addTea) {

    this.userRepository = userRepository;
    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.tagRepository = tagRepository;
    this.addTea = addTea;
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
  ) throws IOException {
    // TODO: form validation (mandatory fields, URL format, etc.)

    var emailAddress = authentication.getName();
    var userId = userRepository.findByEmailIgnoreCase(emailAddress)
      .orElseThrow(() -> new IllegalArgumentException("User not found with email address: " + emailAddress))
      .getId();

    var command = new AddTeaCommand(
      title,
      name,
      description,
      url,
      new TeaScope(season, cultivar, origin, elevation),
      price != null ? new Price(price) : null,
      brewingInstructions,
      true,
      userId,
      vendorId,
      new HashSet<>(teaTypeIds),
      new HashSet<>(tagIds != null ? tagIds : emptyList()),
      compressImages(images));

    return "redirect:/teas/" + this.addTea.handle(command);
  }

  private List<byte[]> compressImages(List<MultipartFile> images) throws IOException {
    var result = new ArrayList<byte[]>();
    if (images != null) {
      for (var imgFile : images) {
        if (!imgFile.isEmpty()) {
          result.add(getCompressedBytes(imgFile));
        }
      }
    }
    return result;
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
