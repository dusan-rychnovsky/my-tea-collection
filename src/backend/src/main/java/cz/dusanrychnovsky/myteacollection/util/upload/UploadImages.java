package cz.dusanrychnovsky.myteacollection.util.upload;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;

public class UploadImages {

  private static final String DB_URL = System.getenv("SPRING_DATASOURCE_URL");
  private static final String DB_USER = System.getenv("SPRING_DATASOURCE_USERNAME");
  private static final String DB_PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD");

  private static final String INPUT_DIR_PATH = "C:\\Users\\durychno\\Dev\\my-tea-collection\\tea";

  public static void main(String[] args) throws SQLException, IOException {
    try (var connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
      var maxTeaId = getMaxTeaId(connection);
      var teas = Tea.loadNewFrom(new File(INPUT_DIR_PATH), maxTeaId + 1);

      try (var insertTeaStmt = new InsertTeaStatement(connection);
           var insertTeaToTeaTypeStmt = new InsertTeaToTeaTypeStatement(connection);
           var insertImgStmt = new InsertTeaImageStatement(connection)) {

        for (var tea : teas) {
          // TODO: proper logging
          System.out.println("Uploading tea: " + tea.getId());

          insertTeaStmt.execute(tea);
          for (var typeId : tea.getTypeIds()) {
            insertTeaToTeaTypeStmt.execute(tea.getId(), typeId);
          }

          var idx = 0;
          for (var image : tea.getImages()) {
            idx++;
            // TODO: load tea images in correct order
            // TODO: jpg compression
            insertImgStmt.execute(tea.getId(), idx, toBytes(image));
          }
        }
      }
    }

    System.out.println("DONE");
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
