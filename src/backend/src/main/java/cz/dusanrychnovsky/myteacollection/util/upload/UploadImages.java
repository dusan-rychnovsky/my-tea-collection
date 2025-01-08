package cz.dusanrychnovsky.myteacollection.util.upload;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;

public class UploadImages {
  public static void main(String[] args) throws SQLException, IOException {

    var INPUT_DIR = "C:\\Users\\durychno\\OneDrive - Microsoft\\Desktop\\img";

    var DB_URL = System.getenv("SPRING_DATASOURCE_URL");
    var DB_USER = System.getenv("SPRING_DATASOURCE_USERNAME");
    var DB_PASSWORD = System.getenv("SPRING_DATASOURCE_PASSWORD");

    try (var connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      var stmt = connection.prepareStatement(
        "INSERT INTO myteacollection.TeaImages (tea_id, index, data)" +
          "VALUES (?, ?, ?)"
      )) {

      var dirPath = Paths.get(INPUT_DIR);
      try (var filePaths = Files.newDirectoryStream(dirPath)) {
        for (var filePath : filePaths) {
          System.out.println("Uploading img: " + filePath);
          uploadImg(filePath.toFile(), stmt);
        }
      }
    }

    System.out.println("DONE");
  }

  private static void uploadImg(File imgFile, PreparedStatement stmt)
    throws IOException, SQLException {

    var tokens = imgFile.getName().split("\\.")[0].split("_");

    var teaId = parseInt(tokens[0]);
    var index = parseInt(tokens[1]);

    var data = toBytes(imgFile);

    stmt.setInt(1, teaId);
    stmt.setInt(2, index);
    stmt.setBytes(3, data);

    stmt.executeUpdate();
  }

  private static byte[] toBytes(File imgFile) throws IOException {
    var extension = getFileExtension(imgFile);
    if (!extension.equals("jpg")) {
      throw new IllegalArgumentException("Only jpg is supported: " + imgFile);
    }
    var bufferedImage = ImageIO.read(imgFile);
    var byteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, extension, byteArrayOutputStream);
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
