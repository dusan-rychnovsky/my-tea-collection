package cz.dusanrychnovsky.myteacollection;

import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.db.users.*;
import cz.dusanrychnovsky.myteacollection.model.Availability;
import cz.dusanrychnovsky.myteacollection.model.FilterCriteria;
import cz.dusanrychnovsky.myteacollection.model.PageInfo;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import cz.dusanrychnovsky.myteacollection.util.upload.JpgCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;

// https://dev.to/philipathanasopoulos/guide-to-free-hosting-for-your-full-stack-spring-boot-application-4fak
// https://spring.io/quickstart
// https://www.baeldung.com/dockerizing-spring-boot-application

@SpringBootApplication
@Controller
public class MyTeaCollectionApplication {

  private static final Logger logger = LoggerFactory.getLogger(MyTeaCollectionApplication.class);

  private static final String REQUEST_PATH_PARAM = "requestPath";
  private static final int PAGE_SIZE = 9;

  public static void main(String[] args) {
    SpringApplication.run(MyTeaCollectionApplication.class, args);
  }

  private final UserRepository userRepository;
  private final VendorRepository vendorRepository;
  private final TeaTypeRepository teaTypeRepository;
  private final TeaImageRepository teaImageRepository;
  private final TeaRepository teaRepository;
  private final TeaSearchRepository teaSearchRepository;
  private final TagRepository tagRepository;

  @Autowired
  public MyTeaCollectionApplication(
    UserRepository userRepository,
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TeaImageRepository teaImageRepository,
    TeaRepository teaRepository,
    TeaSearchRepository teaSearchRepository,
    TagRepository tagRepository) {

    this.userRepository = userRepository;
    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.teaImageRepository = teaImageRepository;
    this.teaRepository = teaRepository;
    this.teaSearchRepository = teaSearchRepository;
    this.tagRepository = tagRepository;
  }

  @GetMapping({"/", "/index"})
  public String index(
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/");
    return handleIndexView(
      model,
      FilterCriteria.EMPTY,
      SearchCriteria.EMPTY,
      pageNo
    );
  }

  @GetMapping("/filter")
  public String filter(
    @ModelAttribute FilterCriteria criteria,
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/filter");
    return handleIndexView(
      model,
      criteria,
      SearchCriteria.EMPTY,
      pageNo
    );
  }

  @GetMapping("/search")
  public String search(
    @ModelAttribute SearchCriteria criteria,
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/search");
    return handleIndexView(
      model,
      FilterCriteria.EMPTY,
      criteria,
      pageNo
    );
  }

  private String handleIndexView(
    Model model, FilterCriteria filterCriteria, SearchCriteria searchCriteria, int pageNo) {

    if (pageNo < 0) {
      pageNo = 0;
    }

    populateDropdowns(model);
    model.addAttribute("filter", filterCriteria);
    model.addAttribute("search", searchCriteria);

    var teas = teaSearchRepository.getPage(filterCriteria, searchCriteria, pageNo, PAGE_SIZE);
    model.addAttribute("teas", teas);

    var totalCount = (int) teaSearchRepository.count(filterCriteria, searchCriteria);
    var pageInfo = new PageInfo(
      pageNo,
      (totalCount + PAGE_SIZE - 1) / PAGE_SIZE
    );
    model.addAttribute("pageInfo", pageInfo);

    return "index";
  }

  private void populateDropdowns(Model model) {
    var allVendors = vendorRepository.findAll();
    allVendors.add(0, new VendorEntity(0L, "All", null));
    model.addAttribute("vendors", allVendors);

    var allTeaTypes = teaTypeRepository.findAll();
    allTeaTypes.add(0, new TeaTypeEntity(0L, "All"));
    model.addAttribute("teaTypes", allTeaTypes);

    var availabilities = Availability.getAll();
    model.addAttribute("availabilities", availabilities);
  }

  @GetMapping("/teas/{id}")
  public String teaView(@PathVariable("id") Long teaId, Model model) {
    var tea = teaRepository.findById(teaId).get();
    model.addAttribute("tea", tea);
    return "tea-view";
  }

  @GetMapping("/images/{id}")
  public ResponseEntity<byte[]> image(@PathVariable("id") Long imgId) {
    return teaImageRepository.findById(imgId)
      .map(image -> {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
      })
      .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
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
