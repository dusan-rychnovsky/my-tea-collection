package cz.dusanrychnovsky.myteacollection.security;

import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailBasedUserDetailsServiceTests {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final EmailBasedUserDetailsService service = new EmailBasedUserDetailsService(userRepository);

  @Test
  void loadUserByUsername_existingEmail_returnsUserDetails() {
    var email = "USER@example.com";
    var user = new UserEntity("user@example.com", "encoded-password", "Test", "User", "Nick", "Prague", "About me");
    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

    var details = service.loadUserByUsername(email);

    assertEquals(user.getEmail(), details.getUsername());
    assertEquals(user.getPassword(), details.getPassword());
    assertEquals(1, details.getAuthorities().size());
    assertEquals("USER", details.getAuthorities().iterator().next().getAuthority());
    verify(userRepository).findByEmailIgnoreCase(email);
  }

  @Test
  void loadUserByUsername_unknownEmail_throwsUsernameNotFound() {
    var email = "missing@example.com";
    when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

    var ex = assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(email));

    assertEquals("User not found with email: " + email, ex.getMessage());
    verify(userRepository).findByEmailIgnoreCase(email);
  }
}
