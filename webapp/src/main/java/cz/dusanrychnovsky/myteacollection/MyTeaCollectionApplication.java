package cz.dusanrychnovsky.myteacollection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// https://dev.to/philipathanasopoulos/guide-to-free-hosting-for-your-full-stack-spring-boot-application-4fak
// https://spring.io/quickstart
// https://www.baeldung.com/dockerizing-spring-boot-application

@SpringBootApplication
public class MyTeaCollectionApplication {

  public static void main(String[] args) {
    SpringApplication.run(MyTeaCollectionApplication.class, args);
  }
}
