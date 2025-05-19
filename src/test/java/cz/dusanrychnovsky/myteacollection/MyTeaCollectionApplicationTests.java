package cz.dusanrychnovsky.myteacollection;

import cz.dusanrychnovsky.myteacollection.util.upload.UploadNewTeas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.TestInstance.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(Lifecycle.PER_CLASS)
class MyTeaCollectionApplicationTests {

	@Autowired
	private UploadNewTeas uploadNewTeas;

	@Autowired
	private MockMvc mvc;

	@BeforeAll
	void setup() throws IOException {
		// insert two teas in the DB
		uploadNewTeas.run(toFile("teas"));
	}

	@Test
	void index_listsAllTeas() throws Exception {
		// request index action
		var actions = mvc.perform(get("/index"))
			.andExpect(status().isOk());

		// verify tea type dropdown
		actions=containsStrings(actions,
			"<option value=\"1\">Blend</option>",
			"<option value=\"2\">White Tea</option>",
			"<option value=\"5\">Oolong</option>",
			"<option value=\"8\">Sheng Puerh</option>",
			"<option value=\"11\">Yabao</option>",
			"<option value=\"12\">Fu Zhuan</option>");

		// verify vendor dropdown
		actions=containsStrings(actions,
			"<option value=\"1\">Mei Leaf</option>",
			"<option value=\"2\">Meetea</option>",
			"<option value=\"5\">Klasek Tea</option>",
			"<option value=\"6\">Banna House</option>");

		// verify availability dropdown
		actions=containsStrings(actions,
			"<option value=\"1\">In stock</option>",
      "<option value=\"2\">Out of stock</option>");

		// verify list of teas
		actions = containsStrings(actions,
			"<h1 class=\"jumbotron-heading\">My Tea Collection</h1>",
			"<span>Doubleshot</span>",
			"<span>Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022</span>",
			"<span>Meetea</span>",
			"<span>Dark Tea, Shu Puerh</span>",
			"<span>Luminary Misfit</span>",
			"<span>Lancang Gushu Sheng PuErh Spring 2022</span>",
			"<span>Mei Leaf</span>",
			"<span>Dark Tea, Sheng Puerh</span>");
	}

	@Test
	void teaView_showsGivenTea() throws Exception {
		// request teaView action
		var actions = mvc.perform(get("/teas/2"))
			.andExpect(status().isOk());

		// verify tea properties
		actions=containsStrings(actions,
			"<span>Luminary Misfit</span>",
			"<div class=\"subtitle mt-1\">Lancang Gushu Sheng PuErh Spring 2022</div>",
			"<li>Dark Tea</li>",
			"<li>Sheng Puerh</li>",
			"<span>Mei Leaf</span>",
			"<a href=\"https://meileaf.com/tea/luminary-misfit/\">meileaf.com</a>",
			"<span>April 2022</span>",
			"<span>Lancang, Puer, Yunnan, China</span>",
			"<span>1740-1970m</span>",
			"<span>Da Ye Zhong</span>",
			"<span>95°C, 5g/100ml, 25+5s</span>");
	}

	private ResultActions containsStrings(ResultActions actions, String... strings) throws Exception {
		for (var str : strings) {
			actions = actions.andExpect(content().string(containsString(str)));
		}
		return actions;
	}
}
