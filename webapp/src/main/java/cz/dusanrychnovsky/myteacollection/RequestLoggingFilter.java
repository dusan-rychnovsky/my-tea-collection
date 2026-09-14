package cz.dusanrychnovsky.myteacollection;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain filterChain
  ) throws ServletException, IOException {
    var startedAt = System.nanoTime();
    var completed = false;

    try {
      filterChain.doFilter(request, response);
      completed = true;
    } finally {
      var durationMillis = NANOSECONDS.toMillis(System.nanoTime() - startedAt);
      var status = completed ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      var requestTarget = request.getRequestURI();
      if (request.getQueryString() != null) {
        requestTarget += "?" + request.getQueryString();
      }

      if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
        logger.error("HTTP {} {} -> {} in {} ms", request.getMethod(), requestTarget, status, durationMillis);
      } else {
        logger.info("HTTP {} {} -> {} in {} ms", request.getMethod(), requestTarget, status, durationMillis);
      }
    }
  }
}