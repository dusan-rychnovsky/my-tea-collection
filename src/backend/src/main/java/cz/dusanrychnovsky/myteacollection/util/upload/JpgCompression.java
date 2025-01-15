package cz.dusanrychnovsky.myteacollection.util.upload;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;

public class JpgCompression {

  private static final float QUALITY = 0.1f; // 0.1 = 10%

  private BufferedImage img;

  public JpgCompression(BufferedImage img) {
    this.img = img;
  }

  public byte[] getBytes() throws IOException {
    var os = new ByteArrayOutputStream();
    var writer = getImageWriter();

    try (var ios = ImageIO.createImageOutputStream(os)) {
      writer.setOutput(ios);

      var param = writer.getDefaultWriteParam();
      if (!param.canWriteCompressed()) {
        throw new IllegalStateException("Jpg writer doesn't support compression.");
      }

      param.setCompressionMode(MODE_EXPLICIT);
      param.setCompressionQuality(QUALITY);

      writer.write(null, new IIOImage(img, null, null), param);
    }
    finally {
      writer.dispose();
    }

    return os.toByteArray();
  }

  private ImageWriter getImageWriter() {
    var writers = ImageIO.getImageWritersByFormatName("jpg");
    if (!writers.hasNext()) {
      throw new IllegalStateException("No jpg writers available.");
    }
    return writers.next();
  }
}
