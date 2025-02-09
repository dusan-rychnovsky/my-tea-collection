package cz.dusanrychnovsky.myteacollection.util.upload;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeaTests {

  @Test
  public void loadFrom_loadsTeaFromGivenDirectory() {
    var tea = Tea.loadFrom(toFile("teas/01"));

    assertEquals(1, tea.getId());
    assertEquals("Doubleshot", tea.getTitle());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", tea.getName());
    assertEquals("Plný, zábavný tmavý čaj se sladce krémovou chutí a vůní třešňového kompotu, čerstvě pražených kávových zrn a svězí, ovocnatou dochutí.", tea.getDescription());
    assertEquals(Set.of(7L, 9L), tea.getTypeIds());
    assertEquals(2, tea.getVendorId());
    assertEquals("https://store.meetea.cz/caj/doubleshot/", tea.getUrl());
    assertEquals("Ming Feng Shan, Yunnan, China", tea.getOrigin());
    assertEquals("Da Ye Zhong", tea.getCultivar());
    assertEquals("Spring and Autumn 2022", tea.getSeason());
    assertEquals("NA", tea.getElevation());
    assertEquals("100°C, 5g/100ml, 15s-10s-5s-10s-20s", tea.getBrewingInstructions());
    assertTrue(tea.isInStock());
    assertEquals(2, tea.getImages().size());
  }

  @Test
  public void loadAllFrom_loadsAllTeasFromGivenRootDirectory() {
    var teas = Tea.loadAllFrom(toFile("teas"));

    assertEquals(2, teas.size());
    assertEquals(1, teas.get(0).getId());
    assertEquals(2, teas.get(1).getId());
  }

  @Test
  public void loadNewFrom_loadsAllTeasFromGivenRootDirectoryWithIdHigherOrEqualsGivenId() {
    var teas = Tea.loadNewFrom(toFile("teas"), 2);

    assertEquals(1, teas.size());
    assertEquals(2, teas.get(0).getId());
  }

  private File toFile(String dirPath) {
    var classLoader = TeaTests.class.getClassLoader();
    var resourceUrl = classLoader.getResource(dirPath);
    return new File(resourceUrl.getFile());
  }
}
