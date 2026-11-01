package cz.dusanrychnovsky.myteacollection.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JpgCompressionTests {

  @Test
  void getBytes_returnsJpegWithOriginalDimensions() throws IOException {
    var img = new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB);

    var bytes = new JpgCompression(img).getBytes();

    assertEquals(0xff, Byte.toUnsignedInt(bytes[0]));
    assertEquals(0xd8, Byte.toUnsignedInt(bytes[1]));
    var decoded = ImageIO.read(new ByteArrayInputStream(bytes));
    assertNotNull(decoded);
    assertEquals(img.getWidth(), decoded.getWidth());
    assertEquals(img.getHeight(), decoded.getHeight());
  }

  @Test
  void getBytes_repeatedCalls_returnSameBytes() throws IOException {
    var compression = new JpgCompression(new BufferedImage(32, 24, BufferedImage.TYPE_INT_RGB));

    assertArrayEquals(compression.getBytes(), compression.getBytes());
  }
}
