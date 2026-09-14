package cz.dusanrychnovsky.myteacollection.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.read.ListAppender;
import cz.dusanrychnovsky.myteacollection.RequestLoggingFilter;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(RequestLoggingIT.TestController.class)
class RequestLoggingIT {

  @Autowired
  private MockMvc mvc;

  private final Logger requestLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;

  @BeforeEach
  void attachLogAppender() {
    originalLevel = requestLogger.getLevel();
    requestLogger.setLevel(Level.INFO);
    appender.list.clear();
    appender.start();
    requestLogger.addAppender(appender);
  }

  @AfterEach
  void detachLogAppender() {
    requestLogger.detachAppender(appender);
    requestLogger.setLevel(originalLevel);
    appender.stop();
  }

  @Test
  void defaultLogging_isInfoLevelWithReadableConsoleFormatting() {
    var context = (LoggerContext) LoggerFactory.getILoggerFactory();
    var rootLogger = context.getLogger(ROOT_LOGGER_NAME);

    assertEquals(Level.INFO, rootLogger.getEffectiveLevel());
    assertEquals(Level.INFO, context.getLogger("org.hibernate.SQL").getEffectiveLevel());
    var consoleAppender = assertInstanceOf(ConsoleAppender.class, rootLogger.getAppender("CONSOLE"));
    assertInstanceOf(PatternLayoutEncoder.class, consoleAppender.getEncoder());
  }

  @Test
  void completedHttpRequest_logsMethodTargetStatusAndDuration() throws Exception {
    mvc.perform(get("/test/request-logging/success").queryParam("source", "integration"))
      .andExpect(status().isNoContent());

    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.INFO, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP GET /test/request-logging/success\\?source=integration -> 204 in \\d+ ms"
    ));
  }

  @Test
  void unhandledEndpointException_logsRequestWithoutDuplicatingStackTrace() {
    var thrown = assertThrows(ServletException.class, () ->
      mvc.perform(get("/test/request-logging/failure"))
    );

    assertInstanceOf(IllegalStateException.class, thrown.getCause());
    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.ERROR, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP GET /test/request-logging/failure -> 500 in \\d+ ms"
    ));
    assertNull(event.getThrowableProxy());
  }

  @RestController
  static class TestController {

    @GetMapping("/test/request-logging/success")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void success() {
    }

    @GetMapping("/test/request-logging/failure")
    void failure() {
      throw new IllegalStateException("Integration test endpoint failed");
    }
  }
}