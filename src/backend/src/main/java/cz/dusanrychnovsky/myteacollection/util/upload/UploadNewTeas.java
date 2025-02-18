package cz.dusanrychnovsky.myteacollection.util.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;

import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.db.Tea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.imageio.ImageIO;

import static cz.dusanrychnovsky.myteacollection.util.upload.Tea.loadNewFrom;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "cz.dusanrychnovsky.myteacollection.db")
@EntityScan(basePackages = "cz.dusanrychnovsky.myteacollection.db")
public class UploadNewTeas {

  private static final String INPUT_DIR_PATH = "teas";

  private static final Logger logger = LoggerFactory.getLogger(UploadNewTeas.class);

  @Autowired
  private VendorRepository vendorRepository;

  @Autowired
  private TeaTypeRepository teaTypeRepository;

  @Autowired
  private TeaRepository teaRepository;

  private Map<String, Vendor> vendors;

  private Map<Long, TeaType> teaTypes;

  public static void main(String[] args) throws IOException {
    logger.info("Starting UploadNewTeas.");
    var context = SpringApplication.run(UploadNewTeas.class, args);
    var bean = context.getBean(UploadNewTeas.class);
    bean.run(new File(args[0]));
    logger.info("UploadNewTeas successfully finished.");
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

    vendors = fetchVendors();
    teaTypes = fetchTeaTypes();

    for (var tea : teas) {
      logger.info("Going to upload tea: #{}", tea.getId());

      var teaEntity = toEntity(tea, vendors, teaTypes);

      var idx = 0;
      // TODO: load tea images in correct order
      for (var image : tea.getImages()) {
        idx++;
        logger.info("Going to upload image: #{}", idx);

        var origBytes = getBytes(image);
        var origLen = origBytes.length;
        var compressedBytes = new JpgCompression(image).getBytes();
        var compressedLen = compressedBytes.length;
        logger.info("JPG compression: original size {}, compressed size {}, ratio {}",
          origLen, compressedLen, (float) compressedLen / origLen);

        var imageEntity = new TeaImage(teaEntity, idx, compressedBytes);
        teaEntity.addImage(imageEntity);
      }

      teaRepository.save(teaEntity);
    }

    logger.info("Upload finished.");
  }

  private long getMaxTeaId() {
    var teas = teaRepository.findAll();
    var latestTea = teas.stream().max(comparingLong(cz.dusanrychnovsky.myteacollection.db.Tea::getId));
    if (latestTea.isPresent()) {
      return latestTea.get().getId();
    }
    return 0L;
  }

  private Map<Long, TeaType> fetchTeaTypes() {
    logger.info("Going to fetch available tea types.");
    return teaTypeRepository.findAll().stream()
      .collect(toMap(TeaType::getId, teaType -> teaType));
  }

  private Map<String, Vendor> fetchVendors() {
    logger.info("Going to fetch available vendors.");
    return vendorRepository.findAll().stream()
      .collect(toMap(Vendor::getName, vendor -> vendor));
  }

  public static Tea toEntity(
    cz.dusanrychnovsky.myteacollection.util.upload.Tea tea,
    Map<String, Vendor> vendors,
    Map<Long, TeaType> teaTypes) {

    var vendor = vendors.get(tea.getVendor());
    if (vendor == null) {
      throw new IllegalArgumentException("Unknown vendor: " + tea.getVendor());
    }

    var types = tea.getTypeIds().stream()
      .map(teaTypes::get)
      .collect(toSet());

    return new Tea(
      tea.getId(),
      vendor,
      types,
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getUrl(),
      tea.getSeason(),
      tea.getOrigin(),
      tea.getElevation(),
      tea.getCultivar(),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      new HashSet<>()
    );
  }

  private static byte[] getBytes(BufferedImage img) throws IOException {
    var os = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", os);
    return os.toByteArray();
  }
}
