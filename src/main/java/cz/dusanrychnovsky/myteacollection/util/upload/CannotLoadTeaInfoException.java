package cz.dusanrychnovsky.myteacollection.util.upload;

import java.io.File;

public class CannotLoadTeaInfoException extends RuntimeException {
  public CannotLoadTeaInfoException(File file, Throwable cause) {
    super("Can't load tea info from file: " + file, cause);
  }
}
