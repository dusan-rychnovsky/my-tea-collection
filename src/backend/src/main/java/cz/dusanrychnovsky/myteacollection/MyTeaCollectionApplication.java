package cz.dusanrychnovsky.myteacollection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Random;

// https://dev.to/philipathanasopoulos/guide-to-free-hosting-for-your-full-stack-spring-boot-application-4fak
// https://spring.io/quickstart
// https://www.baeldung.com/dockerizing-spring-boot-application

@SpringBootApplication
@Controller
public class MyTeaCollectionApplication {

  public static void main(String[] args) {
    SpringApplication.run(MyTeaCollectionApplication.class, args);
  }

  @Autowired
  private TestRepository testRepository;

  @GetMapping("/hello")
  public String hello(Model model) {
    var all = testRepository.findAll();
    var rnd = all.get(new Random().nextInt(all.size()));
    var name = rnd.getTest();
    model.addAttribute("message", String.format("Hello %s!", name));
    return "hello";
  }
}
