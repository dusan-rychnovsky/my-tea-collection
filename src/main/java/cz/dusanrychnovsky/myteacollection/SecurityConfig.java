package cz.dusanrychnovsky.myteacollection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable) // TODO: only temporary, to unblock POST requests for testing
      .authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll()
      )
      .formLogin(form -> form
        .loginPage("/login")
        .permitAll()
      )
      .logout(logout -> logout
        .logoutSuccessUrl("/")
        .permitAll()
      );
    return http.build();
  }
}
