package cz.dusanrychnovsky.myteacollection;

import cz.dusanrychnovsky.myteacollection.db.TeaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
  private TeaRepository teaRepository;

  @GetMapping({"/", "/index"})
  public String index(Model model) {
    var allTeas = teaRepository.findAll();
    model.addAttribute("teas", allTeas);
    return "index";
  }

  @GetMapping("/teas/{id}")
  public String teaView(@PathVariable("id") Long teaId, Model model) {
    var tea = teaRepository.findById(teaId).get();
    model.addAttribute("tea", tea);
    return "tea-view";
  }
}
