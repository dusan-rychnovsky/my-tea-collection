package cz.dusanrychnovsky.myteacollection.util.upload;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cz.dusanrychnovsky.myteacollection.db.TeaEntity;
import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.Transactional;

import static cz.dusanrychnovsky.myteacollection.util.upload.TeaRecord.loadAllFrom;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@SpringBootApplication(scanBasePackages = "cz.dusanrychnovsky.myteacollection")
public class UpdateTeasAvailability {

  private static final Logger logger = LoggerFactory.getLogger(UpdateTeasAvailability.class);

  private final TeaRepository teaRepository;

  public static void main(String[] args) {
    logger.info("Starting UpdateTeasAvailability.");
    var context = SpringApplication.run(UpdateTeasAvailability.class, args);
    var bean = context.getBean(UpdateTeasAvailability.class);
    bean.run(new File(args[0]));
    logger.info("UpdateTeasAvailability successfully finished.");
  }

  @Autowired
  public UpdateTeasAvailability(TeaRepository teaRepository) {
    this.teaRepository = teaRepository;
  }

  @Transactional
  public void run(File rootDir) {
    logger.info("Going to update tea availability from dir: {}.", rootDir);

    var records = loadAllFrom(rootDir);
    logger.info("Loaded {} tea records from disk.", records.size());

    logger.info("Going to fetch teas from db.");
    var teasById = teaRepository.findAll().stream()
      .collect(toMap(TeaEntity::getId, identity()));

    var updates = computeUpdates(records, teasById);

    for (var update : updates.entrySet()) {
      logger.info("Going to update tea: #{} -> inStock={}", update.getKey(), update.getValue());
      teaRepository.updateInStock(update.getKey(), update.getValue());
    }

    logger.info("Update finished. Checked {} teas, updated {}.", records.size(), updates.size());
  }

  static Map<Long, Boolean> computeUpdates(
    List<TeaRecord> records,
    Map<Long, TeaEntity> teasById) {

    var updates = new LinkedHashMap<Long, Boolean>();
    for (var record : records) {
      var entity = teasById.get(record.getId());
      if (entity == null) {
        throw new IllegalStateException("No tea in db for id: " + record.getId());
      }
      if (record.isInStock() != entity.isInStock()) {
        updates.put(record.getId(), record.isInStock());
      }
    }
    return updates;
  }
}
