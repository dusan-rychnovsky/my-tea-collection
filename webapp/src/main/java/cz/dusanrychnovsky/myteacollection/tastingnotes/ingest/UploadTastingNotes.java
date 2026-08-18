package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import java.io.File;

import cz.dusanrychnovsky.myteacollection.tastingnotes.application.ReplaceTeaTastingNotes;
import cz.dusanrychnovsky.myteacollection.tea.ingest.UploadNewTeas;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static java.lang.Long.parseLong;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingLong;

/**
 * CLI batch app that loads tasting notes from a tea-collection directory. For each tea folder it
 * reads {@code tasting-notes.json} and replaces that tea's notes (the files are the source of
 * truth, so re-runs are idempotent). A folder without the file is skipped, keeping its existing
 * notes. The tea is resolved by folder id — folder↔DB id alignment is a documented assumption of
 * this single-maintainer, import-in-order dataset. All notes are owned by the single user.
 *
 * <p>Like {@code CreateUser}, this app declares no {@code @EnableJpaRepositories}/{@code @EntityScan}
 * of its own — those are contributed by {@code UploadNewTeas}, picked up via component scanning of
 * the shared base package (declaring them again would duplicate the repository bean definitions).
 */
@SpringBootApplication(scanBasePackages = "cz.dusanrychnovsky.myteacollection")
public class UploadTastingNotes {

  private static final Logger logger = LoggerFactory.getLogger(UploadTastingNotes.class);

  public static final String USER_EMAIL = UploadNewTeas.USER_EMAIL;

  private final UserRepository userRepository;
  private final ReplaceTeaTastingNotes replaceTeaTastingNotes;

  public static void main(String[] args) {
    logger.info("Starting UploadTastingNotes.");
    var context = SpringApplication.run(UploadTastingNotes.class, args);
    var bean = context.getBean(UploadTastingNotes.class);
    try {
      bean.run(new File(args[0]));
      logger.info("UploadTastingNotes successfully finished.");
    }
    catch (IllegalArgumentException ex) {
      logger.error("UploadTastingNotes aborted; tasting notes were rejected (see the error above).");
      System.exit(1);
    }
  }

  @Autowired
  public UploadTastingNotes(
    UserRepository userRepository, ReplaceTeaTastingNotes replaceTeaTastingNotes) {

    this.userRepository = userRepository;
    this.replaceTeaTastingNotes = replaceTeaTastingNotes;
  }

  public void run(File rootDir) {
    logger.info("Going to upload tasting notes to db from dir: {}.", rootDir);

    var userId = fetchUser().getId();
    var teaDirs = stream(rootDir.listFiles())
      .filter(File::isDirectory)
      .sorted(comparingLong(dir -> parseLong(dir.getName())))
      .toList();

    for (var dir : teaDirs) {
      var teaId = parseLong(dir.getName());
      var records = TastingNoteRecord.loadFrom(dir);
      if (records.isEmpty()) {
        logger.info("No tasting notes file for tea #{}. Skipped.", teaId);
        continue;
      }
      logger.info("Going to replace tasting notes for tea #{} ({} notes).", teaId, records.get().size());
      try {
        replaceTeaTastingNotes.handle(TastingNoteRecordMapper.toCommand(teaId, userId, records.get()));
      }
      catch (IllegalArgumentException ex) {
        logger.error("Failed to load tasting notes for tea #{}: {}", teaId, ex.getMessage());
        throw ex;
      }
    }

    logger.info("Upload finished.");
  }

  private UserEntity fetchUser() {
    return userRepository.findByEmailIgnoreCase(USER_EMAIL)
      .orElseThrow(() -> new IllegalStateException("User not found with email address: " + USER_EMAIL));
  }
}
