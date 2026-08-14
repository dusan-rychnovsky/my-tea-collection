package cz.dusanrychnovsky.myteacollection.domain;

import java.util.List;
import java.util.Set;

/**
 * Write-side aggregate for a tea being added. Holds the tea's descriptive data, its
 * references to reference data (owner, vendor, types, tags) as ids, and its ordered image
 * bytes. Absence of a price is a {@code null} {@link Price}.
 */
public final class Tea {

  private final String title;
  private final String name;
  private final String description;
  private final String url;
  private final TeaScope scope;
  private final Price price;
  private final String brewingInstructions;
  private final boolean inStock;
  private final Long userId;
  private final Long vendorId;
  private final Set<Long> typeIds;
  private final Set<Long> tagIds;
  private final List<byte[]> images;

  public Tea(
    String title,
    String name,
    String description,
    String url,
    TeaScope scope,
    Price price,
    String brewingInstructions,
    boolean inStock,
    Long userId,
    Long vendorId,
    Set<Long> typeIds,
    Set<Long> tagIds,
    List<byte[]> images) {

    this.title = title;
    this.name = name;
    this.description = description;
    this.url = url;
    this.scope = scope;
    this.price = price;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;
    this.userId = userId;
    this.vendorId = vendorId;
    this.typeIds = typeIds;
    this.tagIds = tagIds;
    this.images = images;
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

  public String getUrl() {
    return url;
  }

  public TeaScope getScope() {
    return scope;
  }

  public Price getPrice() {
    return price;
  }

  public String getBrewingInstructions() {
    return brewingInstructions;
  }

  public boolean isInStock() {
    return inStock;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getVendorId() {
    return vendorId;
  }

  public Set<Long> getTypeIds() {
    return typeIds;
  }

  public Set<Long> getTagIds() {
    return tagIds;
  }

  public List<byte[]> getImages() {
    return images;
  }
}
