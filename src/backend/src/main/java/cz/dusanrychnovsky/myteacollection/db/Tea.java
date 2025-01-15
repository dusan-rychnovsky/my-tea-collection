package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparingInt;

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

  /*
  INSERT INTO myteacollection.Teas
  (id, vendor_id, title, name, description, url, season, origin, elevation, cultivar, brewing_instructions)
  VALUES
  (
    1,
    1,
    'Find Your Sunshine 2',
    '2018 Mengku Shu & 2014 Xinhui Chen Pi',
    'Mengku old tree ripe PuErh blended with aged mandarin peel. White chocolate, apple cellars, chamomile flowers, mandarin and cocoa.',
    'https://meileaf.com/tea/find-your-sunshine-2/',
    'Spring 2018',
    'Mengku, Lincang, Yunan, China',
    '1450m',
    'Da Ye Zhong',
    '99°C, 5g/100ml, 20s+5s'
  );

  INSERT INTO myteacollection.teas_TeaTypes (tea_id, type_id)
  VALUES (1, 1), (1, 7), (1, 9);

  INSERT INTO myteacollection.Teas
  (id, vendor_id, title, name, description, url, season, origin, elevation, cultivar, brewing_instructions)
  VALUES
  (
    2,
    1,
    'Souchong Liquor',
    'Tong Mu Zhengshan Xiaozhong',
    'An unsmoked Lapsang that shows the true depth of flavour of this famous tea. Dark cocoa, charred bourbon casks and rambutan.',
    'https://meileaf.com/tea/souchong-liquor/',
    'April 2024',
    'Zhengshan, Fujian, China',
    '1500m',
    'Xingcun Xiao Zhong',
    '90°C, 4g/100ml, 25s+5s'
  );

  INSERT INTO myteacollection.teas_TeaTypes (tea_id, type_id)
  VALUES (2, 6);

  INSERT INTO myteacollection.Teas
  (id, vendor_id, title, name, description, url, season, origin, elevation, cultivar, brewing_instructions)
  VALUES
  (
    3,
    1,
    'Simple Dreams 2',
    '2021 Zhenghe Shou Mei Blend',
    'An experimental blend of ''Green and Brown'' Zhenghe White tea. Strawberry jam on buttered raisin scones, elderflower, honey, warm milk and lotus.',
    'https://meileaf.com/tea/simple-dreams-2/',
    '15th April 2021',
    'Zhenghe, Fujian, China',
    '450m',
    'Da Hao',
    '99°C, 4g/100ml, 20s+5s'
  );

  INSERT INTO myteacollection.teas_TeaTypes (tea_id, type_id)
  VALUES (3, 2);

  INSERT INTO myteacollection.Teas
  (id, vendor_id, title, name, description, url, season, origin, elevation, cultivar, brewing_instructions)
  VALUES
  (
    4,
    1,
    'Royal Peach Orchid',
    'Mi Lan Xiang Dan Cong',
    'Known as a doppelganger tea for its ability to imitate surrounding plants. Forest honey, sweet potato and orchid. Ripe pear with a rocky mineral finish and teasing acidity.',
    'https://meileaf.com/tea/royal-peach-orchid/',
    'March 2024',
    'Fengxi Village, Guangdong, China',
    '900m',
    'Mi Lan',
    '95°C, 5g/100ml, 25s+5s'
  );

  INSERT INTO myteacollection.teas_TeaTypes (tea_id, type_id)
  VALUES (4, 5);
   */
}
