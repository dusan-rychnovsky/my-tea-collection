package cz.dusanrychnovsky.myteacollection.util.upload;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeaTests {
  @Test
  public void loadFrom_loadsTeaFromGivenFolder() throws IOException {
    var tea = Tea.loadFrom(toFile("teas/01"));

    assertEquals("Doubleshot", tea.getTitle());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", tea.getName());
    assertEquals("Plný, zábavný tmavý čaj se sladce krémovou chutí a vůní třešňového kompotu, čerstvě pražených kávových zrn a svězí, ovocnatou dochutí.", tea.getDescription());
    assertEquals(Set.of(7, 9), tea.getTypeIds());
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

  private File toFile(String dirPath) {
    var classLoader = TeaTests.class.getClassLoader();
    var resourceUrl = classLoader.getResource(dirPath);
    return new File(resourceUrl.getFile());
  }
}
