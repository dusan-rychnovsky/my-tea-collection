package cz.dusanrychnovsky.myteacollection.util.upload;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadNewTeas {

  private static final Logger logger = LoggerFactory.getLogger(UploadNewTeas.class);

  private static final String DB_URL = System.getenv("SPRING_DATASOURCE_URL");
  private static final String DB_USER = System.getenv("SPRING_DATASOURCE_USERNAME");
  private static final String DB_PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD");

  private static final String INPUT_DIR_PATH = "C:\\Users\\durychno\\Dev\\my-tea-collection\\tea";

  public static void main(String[] args) throws SQLException, IOException {
    logger.info("Going to upload new teas to production db.");
    try (var connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
      var maxTeaId = getMaxTeaId(connection);
      logger.info("max tea id: {}", maxTeaId);
      var teas = Tea.loadNewFrom(new File(INPUT_DIR_PATH), maxTeaId + 1);

      try (var insertTeaStmt = new InsertTeaStatement(connection);
           var insertTeaToTeaTypeStmt = new InsertTeaToTeaTypeStatement(connection);
           var insertImgStmt = new InsertTeaImageStatement(connection)) {

        for (var tea : teas) {
          logger.info("Going to upload tea: #{}", tea.getId());

          insertTeaStmt.execute(tea);
          for (var typeId : tea.getTypeIds()) {
            logger.info("Going to upload type id mapping: {}", typeId);
            insertTeaToTeaTypeStmt.execute(tea.getId(), typeId);
          }

          var idx = 0;
          for (var image : tea.getImages()) {
            idx++;
            logger.info("Going to upload image: #{}", idx);
            // TODO: load tea images in correct order
            // TODO: jpg compression
            insertImgStmt.execute(tea.getId(), idx, toBytes(image));
          }
        }
      }
    }
    logger.info("Upload finished.");
  }

  private static Integer getMaxTeaId(Connection connection) throws SQLException {
    try (var stmt = connection.prepareStatement("SELECT MAX(id) AS maxId FROM myteacollection.Teas");
      var rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("maxId");
        }
      return 0;
    }
  }

  private static byte[] toBytes(BufferedImage img) throws IOException {
    var byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", byteArrayOutputStream);
    return byteArrayOutputStream.toByteArray();
  }
}
