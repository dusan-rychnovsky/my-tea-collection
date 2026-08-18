package cz.dusanrychnovsky.myteacollection.tastingnotes.ingest;

import java.io.File;

public class CannotLoadTastingNotesException extends RuntimeException {
  public CannotLoadTastingNotesException(File file, Throwable cause) {
    super("Can't load tasting notes from file: " + file, cause);
  }
}
