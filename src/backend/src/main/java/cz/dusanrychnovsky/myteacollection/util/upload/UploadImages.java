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

      try (var insertTeaStmt = connection.prepareStatement(
        "INSERT INTO myteacollection.Teas " +
          "(id, title, name, description, vendor_id, url, origin, cultivar, season, elevation, brewing_instructions, in_stock)" +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
           var insertTeaTypeStmt = connection.prepareStatement(
             "INSERT INTO myteacollection.Teas_TeaTypes (tea_id, type_id) VALUES (?, ?)"
           );
        var insertImgStmt = connection.prepareStatement(
          "INSERT INTO myteacollection.TeaImages (tea_id, index, data)" +
            "VALUES (?, ?, ?)"
        )) {
        for (var tea : teas) {
          uploadTea(tea, insertTeaStmt, insertTeaTypeStmt, insertImgStmt);
        }
      }
    }

    System.out.println("DONE");
  }

  private static void uploadTea(Tea tea, PreparedStatement insertTeaStmt, PreparedStatement insertTeaTypeStmt, PreparedStatement insertImgStmt)
    throws SQLException, IOException {

    System.out.println("Uploading tea: " + tea.getId());

    // TODO: refactor so that creating prepared statement and executing it are colocated
    insertTeaStmt.setInt(1, tea.getId());
    insertTeaStmt.setString(2, tea.getTitle());
    insertTeaStmt.setString(3, tea.getName());
    insertTeaStmt.setString(4, tea.getDescription());
    insertTeaStmt.setInt(5, tea.getVendorId());
    insertTeaStmt.setString(6, tea.getUrl());
    insertTeaStmt.setString(7, tea.getOrigin());
    insertTeaStmt.setString(8, tea.getCultivar());
    insertTeaStmt.setString(9, tea.getSeason());
    insertTeaStmt.setString(10, tea.getElevation());
    insertTeaStmt.setString(11, tea.getBrewingInstructions());
    insertTeaStmt.setBoolean(12, tea.isInStock());

    insertTeaStmt.execute();

    for (var typeId : tea.getTypeIds()) {
      insertTeaTypeStmt.setInt(1, tea.getId());
      insertTeaTypeStmt.setInt(2, typeId);
      insertTeaTypeStmt.execute();
    }

    // TODO: load tea images in correct order
    // TODO: jpg compression
    var idx = 0;
    for (var img : tea.getImages()) {
      idx++;
      insertImgStmt.setInt(1, tea.getId());
      insertImgStmt.setInt(2, idx);
      insertImgStmt.setBytes(3, toBytes(img));

      insertImgStmt.executeUpdate();
    }
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

  private static String getFileExtension(File file) {
    String fileName = file.getName();
    int lastIndexOfDot = fileName.lastIndexOf('.');
    if (lastIndexOfDot == -1) {
      throw new IllegalArgumentException("File has empty extension: " + fileName);
    }
    return fileName.substring(lastIndexOfDot + 1);
  }
}
