package cz.dusanrychnovsky.myteacollection.integration;

import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class AuthenticationIT {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private UserRepository userRepository;

  private static final String TEST_EMAIL = "testuser@example.com";
  private static final String TEST_PASSWORD = "password";

  @BeforeAll
  void insertTestUser() {
    userRepository.deleteAll();
    var encodedPassword = new BCryptPasswordEncoder().encode(TEST_PASSWORD);
    var user = new UserEntity(TEST_EMAIL, encodedPassword, "Test", "User", "Nick", "Prague", "About me");
    userRepository.save(user);
  }

  @AfterAll
  void cleanupTestUser() {
    userRepository.deleteAll();
  }

  @Test
  void login_correctCredentials_thenLogout() throws Exception {
    // not authenticated
    mvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Log In")))
      .andExpect(content().string(not(containsString("Log Out"))));

    // log-in
    var loginResult = mvc.perform(formLogin().user(TEST_EMAIL).password(TEST_PASSWORD))
      .andExpect(status().is3xxRedirection())
      .andReturn();
    var session = (MockHttpSession) loginResult.getRequest().getSession(false);
    assertNotNull(session);

    // authenticated
    mvc.perform(get("/").session(session))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Log Out")))
      .andExpect(content().string(not(containsString("Log In"))));

    // log out
    mvc.perform(post("/logout").with(csrf()).session(session))
      .andExpect(status().is3xxRedirection());

    // not authenticated
    mvc.perform(get("/"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Log In")))
      .andExpect(content().string(not(containsString("Log Out"))));
  }

  @Test
  void login_withWrongPassword_showsError() throws Exception {
    var result = mvc.perform(formLogin().user(TEST_EMAIL).password("wrongpassword"))
      .andExpect(status().is3xxRedirection())
      .andExpect(r ->
        assertNotNull(r.getResponse().getRedirectedUrl())
      )
      .andReturn();

    assertNotNull(result.getResponse().getRedirectedUrl());
    mvc.perform(get(result.getResponse().getRedirectedUrl()))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Invalid email or password.")));
  }
}
