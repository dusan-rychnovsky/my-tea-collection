package cz.dusanrychnovsky.myteacollection.tea.web;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaRepository;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeEntity;
import cz.dusanrychnovsky.myteacollection.persistence.TeaTypeRepository;
import cz.dusanrychnovsky.myteacollection.persistence.VendorEntity;
import cz.dusanrychnovsky.myteacollection.persistence.VendorRepository;
import cz.dusanrychnovsky.myteacollection.tea.query.Availability;
import cz.dusanrychnovsky.myteacollection.tea.query.FilterCriteria;
import cz.dusanrychnovsky.myteacollection.tea.query.PageInfo;
import cz.dusanrychnovsky.myteacollection.tea.query.SearchCriteria;
import cz.dusanrychnovsky.myteacollection.tastingnotes.query.RatingSummary;
import cz.dusanrychnovsky.myteacollection.tastingnotes.query.TastingNoteItem;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaDetail;
import cz.dusanrychnovsky.myteacollection.tea.query.TeaQueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath;

@Controller
public class TeaQueryController {

  private static final String REQUEST_PATH_PARAM = "requestPath";
  private static final String PAGE_SIZE ="9";

  private final VendorRepository vendorRepository;
  private final TeaTypeRepository teaTypeRepository;
  private final TeaRepository teaRepository;
  private final TeaQueryRepository teaQueryRepository;
  private final TastingNoteRepository tastingNoteRepository;

  @Autowired
  public TeaQueryController(
    VendorRepository vendorRepository,
    TeaTypeRepository teaTypeRepository,
    TeaRepository teaRepository,
    TeaQueryRepository teaQueryRepository,
    TastingNoteRepository tastingNoteRepository) {

    this.vendorRepository = vendorRepository;
    this.teaTypeRepository = teaTypeRepository;
    this.teaRepository = teaRepository;
    this.teaQueryRepository = teaQueryRepository;
    this.tastingNoteRepository = tastingNoteRepository;
  }

  @GetMapping({"/", "/index"})
  public String index(
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) int pageSize,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/");
    return handleIndexView(
      model,
      FilterCriteria.EMPTY,
      SearchCriteria.EMPTY,
      pageNo,
      pageSize
    );
  }

  @GetMapping("/filter")
  public String filter(
    @ModelAttribute FilterCriteria criteria,
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) int pageSize,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/filter");
    return handleIndexView(
      model,
      criteria,
      SearchCriteria.EMPTY,
      pageNo,
      pageSize
    );
  }

  @GetMapping("/search")
  public String search(
    @ModelAttribute SearchCriteria criteria,
    @RequestParam(value = "pageNo", defaultValue = "0") int pageNo,
    @RequestParam(value = "pageSize", defaultValue = PAGE_SIZE) int pageSize,
    Model model) {
    model.addAttribute(REQUEST_PATH_PARAM, "/search");
    return handleIndexView(
      model,
      FilterCriteria.EMPTY,
      criteria,
      pageNo,
      pageSize
    );
  }

  private String handleIndexView(
    Model model, FilterCriteria filterCriteria, SearchCriteria searchCriteria, int pageNo, int pageSize) {

    if (pageNo < 0) {
      pageNo = 0;
    }

    populateDropdowns(model);
    model.addAttribute("filter", filterCriteria);
    model.addAttribute("search", searchCriteria);

    var teas = teaQueryRepository.getPage(filterCriteria, searchCriteria, pageNo, pageSize);
    model.addAttribute("teas", teas);

    var totalCount = (int) teaQueryRepository.count(filterCriteria, searchCriteria);
    var pageInfo = new PageInfo(
      pageNo,
      (totalCount + pageSize - 1) / pageSize
    );
    model.addAttribute("pageInfo", pageInfo);

    return "index";
  }

  private void populateDropdowns(Model model) {
    var allVendors = vendorRepository.findAll();
    allVendors.add(0, new VendorEntity(0L, "All", null));
    model.addAttribute("vendors", allVendors);

    var allTeaTypes = teaTypeRepository.findAll();
    allTeaTypes.add(0, new TeaTypeEntity(0L, "All"));
    model.addAttribute("teaTypes", allTeaTypes);

    var availabilities = Availability.getAll();
    model.addAttribute("availabilities", availabilities);
  }

  @GetMapping("/teas/{id}")
  public String teaView(@PathVariable("id") Long teaId, Model model) {
    var tea = teaRepository.findById(teaId).map(TeaDetail::from).get();
    var notes = tastingNoteRepository.findByTeaIdNewestFirst(teaId);
    var baseUrl = fromCurrentContextPath().build().toUriString();
    model.addAttribute("tea", tea);
    model.addAttribute("tastingNotes", notes.stream().map(TastingNoteItem::from).toList());
    model.addAttribute("ratingSummary", RatingSummary.of(notes));
    model.addAttribute("baseUrl", baseUrl);
    return "tea-view";
  }
}
