package cz.dusanrychnovsky.myteacollection.util.upload;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.Integer.parseInt;

public class Tea {

  public static final String INFO_FILE_NAME = "info.json";

  private Integer id;
  private String title;
  private String name;
  private String description;
  private Set<Integer> typeIds;
  private Integer vendorId;
  private String url;
  private String origin;
  private String cultivar;
  private String season;
  private String elevation;
  private String brewingInstructions;
  private boolean inStock;
  private List<Image> images;

  @JsonCreator
  public Tea(
    @JsonProperty(value = "title", required = true) String title,
    @JsonProperty(value = "name", required = true) String name,
    @JsonProperty(value = "description", required = true) String description,
    @JsonProperty(value = "typeIds", required = true) Set<Integer> typeIds,
    @JsonProperty(value = "vendorId", required = true) Integer vendorId,
    @JsonProperty(value = "url", required = true) String url,
    @JsonProperty(value = "origin", required = true) String origin,
    @JsonProperty(value = "cultivar", required = true) String cultivar,
    @JsonProperty(value = "season", required = true) String season,
    @JsonProperty(value = "elevation", required = true) String elevation,
    @JsonProperty(value = "brewingInstructions", required = true) String brewingInstructions,
    @JsonProperty(value = "inStock", required = true) boolean inStock
  ) {
    this.title = title;
    this.name = name;
    this.description = description;
    this.typeIds = typeIds;
    this.vendorId = vendorId;
    this.url = url;
    this.origin = origin;
    this.cultivar = cultivar;
    this.season = season;
    this.elevation = elevation;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;

    images = new ArrayList<>();
  }

  public static Tea loadFrom(File dir) throws IOException {
    var tea = loadInfo(new File(dir, INFO_FILE_NAME));
    tea.id = parseInt(dir.getName());
    for (var file : dir.listFiles()) {
      if (file.getName().equals(INFO_FILE_NAME)) {
        continue;
      }
      tea.images.add(loadImg(file));
    }
    return tea;
  }

  private static Tea loadInfo(File infoFile) throws IOException {
    var mapper = new ObjectMapper();
    return mapper.readValue(infoFile, Tea.class);
  }

  private static Image loadImg(File imgFile) throws IOException {
    return ImageIO.read(imgFile);
  }

  public Integer getId() {
    return id;
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

  public Set<Integer> getTypeIds() {
    return typeIds;
  }

  public Integer getVendorId() {
    return vendorId;
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

  public String getBrewingInstructions() {
    return brewingInstructions;
  }

  public boolean isInStock() {
    return inStock;
  }

  public List<Image> getImages() {
    return images;
  }
}
