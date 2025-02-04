package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparingInt;

// TODO: see how many queries get generated to fetch a tea or list of teas
@Entity
@Table(schema = "myteacollection", name = "Teas")
public class Tea {

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
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  @ManyToMany
  @JoinTable(
    schema = "myteacollection",
    name = "Teas_TeaTypes",
    joinColumns = @JoinColumn(name = "tea_id", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "type_id", referencedColumnName = "id"))
  private Set<TeaType> types;

  private String title;

  private String name;

  private String description;

  @OneToMany
  @JoinColumn(name = "tea_id")
  private Set<TeaImage> images;

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
  private Set<Tag> tags;

  public Long getId() {
    return id;
  }

  public Vendor getVendor() {
    return vendor;
  }

  public Set<TeaType> getTypes() {
    return types;
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

  public Set<TeaImage> getImages() {
    return images;
  }

  public Tea setImages(Set<TeaImage> images) {
    this.images = images;
    return this;
  }

  public Optional<TeaImage> getMainImage() {
    return images.stream()
      .min(comparingInt(TeaImage::getIndex));
  }

  public Set<TeaImage> getAdditionalImages() {
    var result = new HashSet<TeaImage>(images);
    var mainImg = getMainImage();
    mainImg.ifPresent(result::remove);
    return result;
  }

  public String getUrl() {
    return url;
  }

  public Tea setUrl(String url) {
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

  public Set<Tag> getTags() {
    return tags;
  }
}
