package cz.dusanrychnovsky.myteacollection.util.users;

import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import cz.dusanrychnovsky.myteacollection.db.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication(scanBasePackages = "cz.dusanrychnovsky.myteacollection")
public class CreateUser {

  private static final Logger logger = LoggerFactory.getLogger(CreateUser.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public CreateUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: <email> <password> <firstname> <lastname>");
      System.exit(1);
    }
    logger.info("Starting CreateUser.");
    var context = SpringApplication.run(CreateUser.class, args);
    var bean = context.getBean(CreateUser.class);
    bean.run(args[0], args[1], args[2], args[3]);
    logger.info("CreateUser successfully finished.");
  }

  public void run(String email, String password, String firstName, String lastName) {
    logger.info("Going to create user with email: {}", email);
    var encodedPassword = passwordEncoder.encode(password);
    var user = new UserEntity(email, encodedPassword, firstName, lastName);
    userRepository.save(user);
    logger.info("User successfully created.");
  }
}
