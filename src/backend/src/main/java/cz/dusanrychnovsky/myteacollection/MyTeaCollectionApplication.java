package cz.dusanrychnovsky.myteacollection;

import cz.dusanrychnovsky.myteacollection.db.*;
import cz.dusanrychnovsky.myteacollection.model.Availability;
import cz.dusanrychnovsky.myteacollection.model.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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
  private VendorRepository vendorRepository;

  @Autowired
  private TeaTypeRepository teaTypeRepository;

  @Autowired
  private TeaRepository teaRepository;

  @Autowired
  private TeaImageRepository teaImageRepository;

  @GetMapping({"/", "/index"})
  public String index(Model model) {
    return search(new SearchCriteria(0, 0, 0), model);
  }

  @PostMapping("/search")
  public String search(@ModelAttribute SearchCriteria criteria, Model model) {
    // search
    var allVendors = vendorRepository.findAll();
    allVendors.add(0, new Vendor(0L, "All", null));
    model.addAttribute("vendors", allVendors);

    var allTeaTypes = teaTypeRepository.findAll();
    allTeaTypes.add(0, new TeaType(0L, "All"));
    model.addAttribute("teaTypes", allTeaTypes);

    var availabilities = Availability.getAll();
    model.addAttribute("availabilities", availabilities);

    model.addAttribute("search", criteria);

    // listing
    var teas = teaRepository.findByCriteria(criteria);
    model.addAttribute("teas", teas);
    return "index";
  }

  @GetMapping("/teas/{id}")
  public String teaView(@PathVariable("id") Long teaId, Model model) {
    var tea = teaRepository.findById(teaId);
    model.addAttribute("tea", tea);
    return "tea-view";
  }

  @GetMapping("/images/{id}")
  public ResponseEntity<byte[]> image(@PathVariable("id") Long imgId) {
    return teaImageRepository.findById(imgId)
      .map(image -> {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(image.getData(), headers, HttpStatus.OK);
      })
      .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }
}
