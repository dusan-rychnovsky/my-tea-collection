package cz.dusanrychnovsky.myteacollection.util.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.imageio.ImageIO;

import static cz.dusanrychnovsky.myteacollection.util.MapUtils.getOrThrow;
import static cz.dusanrychnovsky.myteacollection.util.MapUtils.mapAll;
import static cz.dusanrychnovsky.myteacollection.util.upload.TeaRecord.loadNewFrom;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toMap;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "cz.dusanrychnovsky.myteacollection.db")
@EntityScan(basePackages = "cz.dusanrychnovsky.myteacollection.db")
public class UploadNewTeas {

  private static final Logger logger = LoggerFactory.getLogger(UploadNewTeas.class);

  private VendorRepository vendorRepository;
  private TeaTypeRepository teaTypeRepository;
  private TagRepository tagRepository;
  private TeaRepository teaRepository;

  private Map<String, VendorEntity> vendors;
  private Map<String, TeaTypeEntity> teaTypes;
  private Map<String, TagEntity> tags;

  public static void main(String[] args) throws IOException {
    logger.info("Starting UploadNewTeas.");
    var context = SpringApplication.run(UploadNewTeas.class, args);
    var bean = context.getBean(UploadNewTeas.class);
    bean.run(new File(args[0]));
    logger.info("UploadNewTeas successfully finished.");
  }

  @Autowired
  public UploadNewTeas(
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TagRepository tagRepository,
    TeaRepository teaRepository) {

    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.tagRepository = tagRepository;
    this.teaRepository = teaRepository;
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
    tags = fetchTags();

    for (var tea : teas) {
      logger.info("Going to upload tea: #{}", tea.getId());

      var teaEntity = toEntity(tea, vendors, teaTypes, tags);

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

        var dataEntity = new TeaImageDataEntity(compressedBytes);
        var imageEntity = new TeaImageEntity(teaEntity, idx, dataEntity);
        teaEntity.addImage(imageEntity);
      }

      teaRepository.save(teaEntity);
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

  public static TeaEntity toEntity(
    TeaRecord tea,
    Map<String, VendorEntity> vendors,
    Map<String, TeaTypeEntity> teaTypes,
    Map<String, TagEntity> tags) {

    var vendorEntity = getOrThrow(vendors, tea.getVendor());
    var typeEntities = mapAll(teaTypes, tea.getTypes());
    var tagEntities = mapAll(tags, tea.getTags());

    return new TeaEntity(
      null,
      vendorEntity,
      typeEntities,
      tea.getTitle(),
      tea.getName(),
      tea.getDescription(),
      tea.getUrl(),
      new TeaScope(
        tea.getSeason(),
        tea.getCultivar(),
        tea.getOrigin(),
        tea.getElevation()
      ),
      tea.getBrewingInstructions(),
      tea.isInStock(),
      tagEntities
    );
  }

  private static byte[] getBytes(BufferedImage img) throws IOException {
    var os = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", os);
    return os.toByteArray();
  }
}
