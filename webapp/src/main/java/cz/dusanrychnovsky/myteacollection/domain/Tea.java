package cz.dusanrychnovsky.myteacollection.domain;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Write-side aggregate for a tea being added: its descriptive data, its mandatory references to
 * reference data (vendor, types, tags) held by id, and its ordered image bytes. Absence of a price
 * is a {@code null} {@link Price}. The owner is deliberately not held here — it is actor context
 * supplied by the {@code AddTea} application service, not a property of the tea itself.
 *
 * <p>The constructor enforces every invariant checkable from the tea's own state, throwing
 * {@link IllegalArgumentException} on violation: {@code title}, {@code description} and {@code url}
 * must be present (and {@code url} well-formed), {@code scope} and {@code vendorId} must be non-null,
 * there must be at least one type and at least one image, and {@code tagIds} must be non-null (but
 * may be empty). {@code name} is optional (may be blank — some teas have only a technical title).
 * <em>Existence</em> of the referenced vendor/types/tags cannot be checked here (it needs a
 * repository) and is validated by {@code AddTea}.
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
    Long vendorId,
    Set<Long> typeIds,
    Set<Long> tagIds,
    List<byte[]> images) {

    if (isBlank(title)) {
      throw new IllegalArgumentException("Tea title must not be blank.");
    }
    if (isBlank(description)) {
      throw new IllegalArgumentException("Tea description must not be blank.");
    }
    requireValidUrl(url);
    if (scope == null) {
      throw new IllegalArgumentException("Tea scope must not be null.");
    }
    if (vendorId == null) {
      throw new IllegalArgumentException("Tea must reference a vendor.");
    }
    if (typeIds == null || typeIds.isEmpty()) {
      throw new IllegalArgumentException("Tea must have at least one type.");
    }
    if (tagIds == null) {
      throw new IllegalArgumentException("Tea tags must not be null.");
    }
    if (images == null || images.isEmpty()) {
      throw new IllegalArgumentException("Tea must have at least one image.");
    }

    this.title = title;
    this.name = name;
    this.description = description;
    this.url = url;
    this.scope = scope;
    this.price = price;
    this.brewingInstructions = brewingInstructions;
    this.inStock = inStock;
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

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static void requireValidUrl(String url) {
    if (isBlank(url)) {
      throw new IllegalArgumentException("Tea url must not be blank.");
    }
    try {
      new URL(url);
    }
    catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Tea url is not a valid URL: " + url, ex);
    }
  }
}
