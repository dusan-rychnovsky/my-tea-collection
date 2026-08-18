package cz.dusanrychnovsky.myteacollection.tea.ingest;

import java.io.File;

public class CannotLoadTeaImageException extends RuntimeException {
  public CannotLoadTeaImageException(File file, Throwable cause) {
    super("Can't load tea image from file: " + file, cause);
  }
}
