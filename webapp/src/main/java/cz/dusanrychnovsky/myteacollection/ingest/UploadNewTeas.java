package cz.dusanrychnovsky.myteacollection.ingest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.dusanrychnovsky.myteacollection.application.AddTea;
import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import cz.dusanrychnovsky.myteacollection.util.JpgCompression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.imageio.ImageIO;

import static cz.dusanrychnovsky.myteacollection.ingest.TeaRecord.loadNewFrom;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toMap;

@SpringBootApplication(scanBasePackages = "cz.dusanrychnovsky.myteacollection")
@EnableJpaRepositories(basePackages = "cz.dusanrychnovsky.myteacollection.db")
@EntityScan(basePackages = "cz.dusanrychnovsky.myteacollection.db")
public class UploadNewTeas {

  private static final Logger logger = LoggerFactory.getLogger(UploadNewTeas.class);

  public static final String USER_EMAIL = "dusan.rychnovsky@gmail.com";

  private final UserRepository userRepository;
  private final VendorRepository vendorRepository;
  private final TeaTypeRepository teaTypeRepository;
  private final TagRepository tagRepository;
  private final TeaRepository teaRepository;
  private final AddTea addTea;

  public static void main(String[] args) throws IOException {
    logger.info("Starting UploadNewTeas.");
    var context = SpringApplication.run(UploadNewTeas.class, args);
    var bean = context.getBean(UploadNewTeas.class);
    try {
      bean.run(new File(args[0]));
      logger.info("UploadNewTeas successfully finished.");
    }
    catch (IllegalArgumentException ex) {
      logger.error("UploadNewTeas aborted; a tea was rejected (see the error above).");
      System.exit(1);
    }
  }

  @Autowired
  public UploadNewTeas(
    UserRepository userRepository,
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TagRepository tagRepository,
    TeaRepository teaRepository,
    AddTea addTea) {

    this.userRepository = userRepository;
    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.tagRepository = tagRepository;
    this.teaRepository = teaRepository;
    this.addTea = addTea;
  }

  public void run(File rootDir) throws IOException {
    logger.info("Going to upload new teas to db from dir: {}.", rootDir);

    var maxTeaId = getMaxTeaId();
    logger.info("max tea id: {}", maxTeaId);

    var teas = loadNewFrom(rootDir, maxTeaId + 1);
    if (teas.isEmpty()) {
      logger.info("No new teas. Upload skipped.");
      return;
    }

    var userId = fetchUser().getId();
    var vendors = fetchVendors();
    var teaTypes = fetchTeaTypes();
    var tags = fetchTags();

    for (var tea : teas) {
      logger.info("Going to upload tea: #{}", tea.getId());
      try {
        var command = TeaRecordMapper.toCommand(userId, tea, compress(tea), vendors, teaTypes, tags);
        addTea.handle(command);
      }
      catch (IllegalArgumentException ex) {
        logger.error("Failed to upload tea #{}: {}", tea.getId(), ex.getMessage());
        throw ex;
      }
    }

    logger.info("Upload finished.");
  }

  private long getMaxTeaId() {
    var teas = teaRepository.findAll();
    var latestTea = teas.stream().max(comparingLong(TeaEntity::getId));
    if (latestTea.isPresent()) {
      return latestTea.get().getId();
    }
    return 0L;
  }


  private UserEntity fetchUser() {
    return userRepository.findByEmailIgnoreCase(USER_EMAIL)
      .orElseThrow(() -> new IllegalStateException("User not found with email address: " + USER_EMAIL));
  }

  private Map<String, TeaTypeEntity> fetchTeaTypes() {
    logger.info("Going to fetch available tea types.");
    return teaTypeRepository.findAll().stream()
      .collect(toMap(TeaTypeEntity::getName, teaType -> teaType));
  }

  private Map<String, VendorEntity> fetchVendors() {
    logger.info("Going to fetch available vendors.");
    return vendorRepository.findAll().stream()
      .collect(toMap(VendorEntity::getName, vendor -> vendor));
  }

  private Map<String, TagEntity> fetchTags() {
    logger.info("Going to fetch available tags.");
    return tagRepository.findAll().stream()
      .collect(toMap(TagEntity::getLabel, tag -> tag));
  }

  private List<byte[]> compress(TeaRecord tea) throws IOException {
    var images = new ArrayList<byte[]>();
    var idx = 0;
    // TODO: load tea images in correct order
    for (var image : tea.loadImages()) {
      idx++;
      logger.info("Going to upload image: #{}", idx);
      var origBytes = getBytes(image);
      var compressedBytes = new JpgCompression(image).getBytes();
      logger.info("JPG compression: original size {}, compressed size {}, ratio {}",
        origBytes.length, compressedBytes.length, (float) compressedBytes.length / origBytes.length);
      images.add(compressedBytes);
    }
    return images;
  }

  private static byte[] getBytes(BufferedImage img) throws IOException {
    var os = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", os);
    return os.toByteArray();
  }
}
