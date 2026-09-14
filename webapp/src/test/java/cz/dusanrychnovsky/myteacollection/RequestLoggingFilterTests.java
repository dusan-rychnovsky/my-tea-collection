package cz.dusanrychnovsky.myteacollection;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestLoggingFilterTests {

  private final RequestLoggingFilter filter = new RequestLoggingFilter();
  private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private Level originalLevel;

  @BeforeEach
  void attachLogAppender() {
    originalLevel = logger.getLevel();
    logger.setLevel(Level.INFO);
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachLogAppender() {
    logger.detachAppender(appender);
    logger.setLevel(originalLevel);
    appender.stop();
  }

  @Test
  void completedRequest_logsMethodTargetStatusAndDuration() throws ServletException, IOException {
    var request = new MockHttpServletRequest("GET", "/teas/jade-star-8");
    request.setQueryString("view=full");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, servletResponse) ->
      ((HttpServletResponse) servletResponse).setStatus(204)
    );

    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.INFO, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP GET /teas/jade-star-8\\?view=full -> 204 in \\d+ ms"
    ));
  }

  @Test
  void unhandledException_logsServerErrorWithoutThrowableAndRethrowsIt() {
    var request = new MockHttpServletRequest("GET", "/teas/broken");
    var response = new MockHttpServletResponse();
    var failure = new IllegalStateException("Cannot render tea");

    var thrown = assertThrows(IllegalStateException.class, () ->
      filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
        throw failure;
      })
    );

    assertSame(failure, thrown);
    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.ERROR, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP GET /teas/broken -> 500 in \\d+ ms"
    ));
    assertNull(event.getThrowableProxy());
  }

  @Test
  void serverErrorResponse_logsAtErrorLevel() throws ServletException, IOException {
    var request = new MockHttpServletRequest("POST", "/teas");
    var response = new MockHttpServletResponse();

    filter.doFilter(request, response, (ignoredRequest, servletResponse) ->
      ((HttpServletResponse) servletResponse).setStatus(503)
    );

    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.ERROR, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP POST /teas -> 503 in \\d+ ms"
    ));
    assertNull(event.getThrowableProxy());
  }

  @Test
  void unhandledError_logsServerErrorWithoutThrowableAndRethrowsIt() {
    var request = new MockHttpServletRequest("GET", "/teas/broken");
    var response = new MockHttpServletResponse();
    var failure = new AssertionError("Rendering failed");

    var thrown = assertThrows(AssertionError.class, () ->
      filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
        throw failure;
      })
    );

    assertSame(failure, thrown);
    assertEquals(1, appender.list.size());
    var event = appender.list.get(0);
    assertEquals(Level.ERROR, event.getLevel());
    assertTrue(event.getFormattedMessage().matches(
      "HTTP GET /teas/broken -> 500 in \\d+ ms"
    ));
    assertNull(event.getThrowableProxy());
  }
}