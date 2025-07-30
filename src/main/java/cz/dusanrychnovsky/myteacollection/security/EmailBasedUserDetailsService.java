package cz.dusanrychnovsky.myteacollection.security;

import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class EmailBasedUserDetailsService implements UserDetailsService {

  private UserRepository userRepository;

  @Autowired
  public EmailBasedUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    var user = userRepository.findByEmailIgnoreCase(email)
      .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    return User.withUsername(user.getEmail())
      .password(user.getPassword())
      .authorities("USER")
      .build();
  }
}
