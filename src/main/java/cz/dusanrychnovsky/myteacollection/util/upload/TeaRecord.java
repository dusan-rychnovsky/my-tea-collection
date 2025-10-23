package cz.dusanrychnovsky.myteacollection.util.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Long.parseLong;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingLong;
import static java.util.Collections.emptySet;

public class TeaRecord {

  public static final String INFO_FILE_NAME = "info.json";

  private Long id;
  private String title;
  private String name;
  private String description;
  private Set<String> types;
  private String vendor;
  private String url;
  private String origin;
  private String cultivar;
  private String season;
  private String elevation;
  private String price;
  private String brewingInstructions;
  private boolean inStock;
  private Set<String> tags;

  @JsonIgnore
  private List<BufferedImage> images;

  @JsonCreator
  public TeaRecord(
    @JsonProperty(value = "title", required = true) String title,
    @JsonProperty(value = "name", required = true) String name,
    @JsonProperty(value = "description", required = true) String description,
    @JsonProperty(value = "types", required = true) Set<String> types,
    @JsonProperty(value = "vendor", required = true) String vendor,
    @JsonProperty(value = "url", required = true) String url,
    @JsonProperty(value = "origin", required = true) String origin,
    @JsonProperty(value = "cultivar", required = true) String cultivar,
    @JsonProperty(value = "season", required = true) String season,
    @JsonProperty(value = "elevation", required = true) String elevation,
    @JsonProperty(value = "price", required = true) String price,
    @JsonProperty(value = "brewingInstructions", required = true) String brewingInstructions,
    @JsonProperty(value = "inStock", required = true) boolean inStock,
    @JsonProperty(value = "tags") Set<String> tags
  ) {
    this.title = title;
    this.name = name;
    this.description = description;
    this.types = types;
    this.vendor = vendor;
    this.url = url;
    this.origin = origin;
    this.cultivar = cultivar;
    this.season = season;
    this.elevation = elevation;
    this.price = price;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;
    this.tags = tags != null ? tags : emptySet();

    images = new ArrayList<>();
  }

  public static List<TeaRecord> loadNewFrom(File rootDir, long minId) {
    return stream(rootDir.listFiles())
      .filter(File::isDirectory)
      .filter(file -> { var id = parseLong(file.getName()); return id >= minId; })
      .map(TeaRecord::loadFrom)
      .sorted(comparingLong(tea -> tea.id))
      .toList();
  }

  public static List<TeaRecord> loadAllFrom(File rootDir) {
    return loadNewFrom(rootDir, 0);
  }
  
  public static TeaRecord loadFrom(File dir) {
    var tea = loadInfo(new File(dir, INFO_FILE_NAME));
    tea.setId(parseLong(dir.getName()));
    for (var file : dir.listFiles()) {
      if (file.getName().equals(INFO_FILE_NAME)) {
        continue;
      }
      tea.images.add(loadImg(file));
    }
    return tea;
  }

  private static TeaRecord loadInfo(File infoFile) {
    var mapper = new ObjectMapper();
    try {
      return mapper.readValue(infoFile, TeaRecord.class);
    }
    catch (IOException ex) {
      throw new CannotLoadTeaInfoException(infoFile, ex);
    }
  }

  private static BufferedImage loadImg(File imgFile) {
    try {
      return ImageIO.read(imgFile);
    }
    catch (IOException ex) {
      throw new CannotLoadTeaImageException(imgFile, ex);
    }
  }

  public long getId() {
    return id;
  }

  public TeaRecord setId(long id) {
    this.id = id;
    return this;
  }

  public String getTitle() {
    return title;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Set<String> getTypes() {
    return types;
  }

  public String getVendor() {
    return vendor;
  }

  public String getUrl() {
    return url;
  }

  public String getOrigin() {
    return origin;
  }

  public String getCultivar() {
    return cultivar;
  }

  public String getSeason() {
    return season;
  }

  public String getElevation() {
    return elevation;
  }

  public String getPrice() {
    return price;
  }

  public String getBrewingInstructions() {
    return brewingInstructions;
  }

  public boolean isInStock() {
    return inStock;
  }

  public List<BufferedImage> getImages() {
    return images;
  }

  public Set<String> getTags() {
    return tags;
  }
}
