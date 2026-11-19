package cz.dusanrychnovsky.myteacollection.tea.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadNewTeasTests {

  @Test
  void parseArguments_withoutReset_keepsIncrementalMode() {
    var arguments = UploadNewTeas.parseArguments(new String[] { "teas" });

    assertEquals(new File("teas"), arguments.rootDir());
    assertFalse(arguments.reset());
  }

  @ParameterizedTest
  @CsvSource({ "--reset, teas", "teas, --reset" })
  void parseArguments_acceptsResetBeforeOrAfterDirectory(String first, String second) {
    var arguments = UploadNewTeas.parseArguments(new String[] { first, second });

    assertEquals(new File("teas"), arguments.rootDir());
    assertTrue(arguments.reset());
  }

  @ParameterizedTest
  @ValueSource(strings = { "--reset=true", "--reset=false", "--reset=" })
  void parseArguments_resetWithValue_isRejected(String flag) {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> UploadNewTeas.parseArguments(new String[] { "teas", flag }));

    assertTrue(ex.getMessage().contains("does not accept a value"));
  }

  @Test
  void parseArguments_requiresExactlyOneDirectory() {
    var ex = assertThrows(IllegalArgumentException.class,
      () -> UploadNewTeas.parseArguments(new String[] {}));
    assertEquals("Usage: UploadNewTeas <root-directory> [--reset]", ex.getMessage());
    assertThrows(IllegalArgumentException.class,
      () -> UploadNewTeas.parseArguments(new String[] { "--reset" }));
    assertThrows(IllegalArgumentException.class,
      () -> UploadNewTeas.parseArguments(new String[] { "teas", "another-directory", "--reset" }));
  }

  @Test
  void parseArguments_allowsSpringConfigurationOptions() {
    var arguments = UploadNewTeas.parseArguments(new String[] {
      "--spring.main.web-application-type=none", "teas", "--reset"
    });

    assertEquals(new File("teas"), arguments.rootDir());
    assertTrue(arguments.reset());
  }
}
