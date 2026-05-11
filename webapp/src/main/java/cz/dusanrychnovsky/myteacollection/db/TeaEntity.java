package cz.dusanrychnovsky.myteacollection.db;

import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import jakarta.persistence.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.joining;

@Entity
@Table(schema = "myteacollection", name = "Teas")
public class TeaEntity {

  private static final String NO_PRICE = "N/A";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;

  @ManyToOne
  @JoinColumn(name = "vendor_id")
  private VendorEntity vendor;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    schema = "myteacollection",
    name = "Teas_TeaTypes",
    joinColumns = @JoinColumn(name = "tea_id", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "type_id", referencedColumnName = "id"))
  private Set<TeaTypeEntity> types = new HashSet<>();

  private String title;

  private String name;

  @Column(columnDefinition = "VARCHAR(2048)")
  private String description;

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "tea_id")
  private Set<TeaImageEntity> images = new HashSet<>();

  private String url;

  @Embedded
  private TeaScope scope;

  @Column(name = "price")
  private Float price;

  @Column(name = "brewing_instructions")
  private String brewingInstructions;

  @Column(name = "in_stock")
  private Boolean inStock;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
    schema = "myteacollection",
    name = "Teas_Tags",
    joinColumns = @JoinColumn(name = "tea_id", referencedColumnName = "id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id", referencedColumnName = "id"))
  private Set<TagEntity> tags;

  public TeaEntity(
    UserEntity user,
    VendorEntity vendor,
    Set<TeaTypeEntity> types,
    String title,
    String name,
    String description,
    String url,
    TeaScope scope,
    Float price,
    String brewingInstructions,
    boolean inStock,
    Set<TagEntity> tags) {

    this.user = user;
    this.vendor = vendor;
    this.types = types;
    this.title = title;
    this.name = name;
    this.description = description;
    this.images = new HashSet<>();
    this.url = url;
    this.scope = scope;
    this.price = price;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;
    this.tags = tags;
  }

  public TeaEntity() {
  }

  public Long getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
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

  public TeaScope getScope() {
    return scope;
  }

  public Float getPrice() {
    return price;
  }

  public TeaEntity setPrice(Float price) {
    this.price = price;
    return this;
  }

  public String printPrice() {
    if (price == null) {
      return NO_PRICE;
    }
    return String.format("%.0f CZK / 50g", price * 50);
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
