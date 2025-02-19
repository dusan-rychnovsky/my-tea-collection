package cz.dusanrychnovsky.myteacollection.util.upload;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static cz.dusanrychnovsky.myteacollection.util.ClassLoaderUtils.toFile;
import static cz.dusanrychnovsky.myteacollection.util.upload.TeaRecord.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TeaRecordTests {

  @Test
  public void loadFrom_loadsTeaFromGivenDirectory() {
    var tea = loadFrom(toFile("teas/01"));

    assertEquals(1, tea.getId());
    assertEquals("Doubleshot", tea.getTitle());
    assertEquals("Ming Feng Shan Lao Shu Shu Puer Bing Cha 2022", tea.getName());
    assertEquals("Plný, zábavný tmavý čaj se sladce krémovou chutí a vůní třešňového kompotu, čerstvě pražených kávových zrn a svězí, ovocnatou dochutí.", tea.getDescription());
    assertEquals(Set.of("Dark", "Shu Puerh"), tea.getTypes());
    assertEquals("Meetea", tea.getVendor());
    assertEquals("https://store.meetea.cz/caj/doubleshot/", tea.getUrl());
    assertEquals("Ming Feng Shan, Yunnan, China", tea.getOrigin());
    assertEquals("Da Ye Zhong", tea.getCultivar());
    assertEquals("Spring and Autumn 2022", tea.getSeason());
    assertEquals("NA", tea.getElevation());
    assertEquals("100°C, 5g/100ml, 15s-10s-5s-10s-20s", tea.getBrewingInstructions());
    assertTrue(tea.isInStock());
    assertEquals(2, tea.getImages().size());
    assertEquals(Set.of("meetea-2025-jan", "meetea-2024-dec"), tea.getTags());
  }

  @Test
  public void loadAllFrom_loadsAllTeasFromGivenRootDirectory() {
    var teas = loadAllFrom(toFile("teas"));

    assertEquals(2, teas.size());
    assertEquals(1, teas.get(0).getId());
    assertEquals(2, teas.get(1).getId());
  }

  @Test
  public void loadAllFrom_tagsAreOptional() {
    var teas = loadAllFrom(toFile("teas"));

    assertEquals(2, teas.size());
    assertEquals(2, teas.get(0).getTags().size());
    assertEquals(0, teas.get(1).getTags().size());
  }

  @Test
  public void loadNewFrom_loadsAllTeasFromGivenRootDirectoryWithIdHigherOrEqualsGivenId() {
    var teas = loadNewFrom(toFile("teas"), 2);

    assertEquals(1, teas.size());
    assertEquals(2, teas.get(0).getId());
  }
}
