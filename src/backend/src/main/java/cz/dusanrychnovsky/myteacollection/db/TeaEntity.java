package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.joining;

// TODO: see how many queries get generated to fetch a tea or list of teas
@Entity
@Table(schema = "myteacollection", name = "Teas")
public class TeaEntity {

  /*
    CREATE TABLE myteacollection.Teas (
      id SERIAL PRIMARY KEY,
      vendor_id BIGINT,
      title VARCHAR(255),
      name VARCHAR(255),
      description TEXT,
      url VARCHAR(255),
      origin VARCHAR(255),
      cultivar VARCHAR(255),
      season VARCHAR(255),
      elevation VARCHAR(255),
      brewing_instructions VARCHAR(255),
      in_stock BOOLEAN,
      FOREIGN KEY (vendor_id) REFERENCES myteacollection.Vendors(id)
    );

    CREATE TABLE myteacollection.Teas_TeaTypes (
      tea_id BIGINT NOT NULL,
      type_id BIGINT NOT NULL,
      PRIMARY KEY (tea_id, type_id),
      FOREIGN KEY (tea_id) REFERENCES myteacollection.Teas(id),
      FOREIGN KEY (type_id) REFERENCES myteacollection.TeaTypes(id)
    );

    CREATE TABLE myteacollection.Teas_Tags (
      tea_id BIGINT NOT NULL,
      tag_id BIGINT NOT NULL,
      PRIMARY KEY (tea_id, tag_id),
      FOREIGN KEY (tea_id) REFERENCES myteacollection.Teas(id),
      FOREIGN KEY (tag_id) REFERENCES myteacollection.Tags(id)
    );
   */

  @Id
  private Long id;

  @ManyToOne
  @JoinColumn(name = "vendor_id")
  private VendorEntity vendor;

  @ManyToMany
  @JoinTable(
    schema = "myteacollection",
    name = "Teas_TeaTypes",
    joinColumns = @JoinColumn(name = "tea_id", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "type_id", referencedColumnName = "id"))
  private Set<TeaTypeEntity> types = new HashSet<>();

  private String title;

  private String name;

  @Column(columnDefinition = "VARCHAR(1024)")
  private String description;

  @OneToMany(cascade = CascadeType.ALL)
  @JoinColumn(name = "tea_id")
  private Set<TeaImageEntity> images = new HashSet<>();

  private String url;

  private String season;

  private String origin;

  private String elevation;

  private String cultivar;

  @Column(name = "brewing_instructions")
  private String brewingInstructions;

  @Column(name = "in_stock")
  private Boolean inStock;

  @ManyToMany
  @JoinTable(
    schema = "myteacollection",
    name = "Teas_Tags",
    joinColumns = @JoinColumn(name = "tea_id", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
  private Set<TagEntity> tags;

  public TeaEntity(
    long id,
    VendorEntity vendor,
    Set<TeaTypeEntity> types,
    String title,
    String name,
    String description,
    String url,
    String season,
    String origin,
    String elevation,
    String cultivar,
    String brewingInstructions,
    boolean inStock,
    Set<TagEntity> tags) {

    this.id = id;
    this.vendor = vendor;
    this.types = types;
    this.title = title;
    this.name = name;
    this.description = description;
    this.images = new HashSet<>();
    this.url = url;
    this.season = season;
    this.origin = origin;
    this.elevation = elevation;
    this.cultivar = cultivar;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;
    this.tags = tags;
  }

  public TeaEntity() {
  }

  public Long getId() {
    return id;
  }

  public VendorEntity getVendor() {
    return vendor;
  }

  public Set<TeaTypeEntity> getTypes() {
    return types;
  }

  public TeaEntity setTypes(Set<TeaTypeEntity> types) {
    this.types = types;
    return this;
  }

  public String printTypes() {
    return types.stream()
      .sorted(comparingLong(TeaTypeEntity::getId))
      .map(TeaTypeEntity::getName)
      .collect(joining(", "));
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

  public Set<TeaImageEntity> getImages() {
    return images;
  }

  public void addImage(TeaImageEntity image) {
    images.add(image);
  }

  public TeaEntity setImages(Set<TeaImageEntity> images) {
    this.images = images;
    return this;
  }

  public Optional<TeaImageEntity> getMainImage() {
    return images.stream()
      .min(comparingInt(TeaImageEntity::getIndex));
  }

  public Set<TeaImageEntity> getAdditionalImages() {
    var result = new HashSet<TeaImageEntity>(images);
    var mainImg = getMainImage();
    mainImg.ifPresent(result::remove);
    return result;
  }

  public String getUrl() {
    return url;
  }

  public TeaEntity setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getUrlDomain() throws MalformedURLException {
    return new URL(url).getHost();
  }

  public String getSeason() {
    return season;
  }

  public String getOrigin() {
    return origin;
  }

  public String getElevation() {
    return elevation;
  }

  public String getCultivar() {
    return cultivar;
  }

  public String getBrewingInstructions() {
    return brewingInstructions;
  }

  public Set<TagEntity> getTags() {
    return tags;
  }

  public boolean isInStock() {
    return inStock;
  }
}
