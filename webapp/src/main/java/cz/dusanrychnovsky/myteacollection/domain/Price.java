package cz.dusanrychnovsky.myteacollection.domain;

import java.util.Optional;

/**
 * A tea price, stored as an amount in CZK per gram and displayed per a 50g serving.
 * The value is non-negative; absence of a price is represented outside this type
 * (a nullable column / an empty {@link Optional}), not by a "no price" instance.
 */
public record Price(float amountPerGram) {

  private static final String ABSENT = "N/A";

  public Price {
    if (amountPerGram < 0) {
      throw new IllegalArgumentException("Price must be non-negative: " + amountPerGram);
    }
  }

  /**
   * Parses the ingest string form: the literal {@code "N/A"} yields an empty result,
   * a numeric string yields a present {@code Price}, anything else throws.
   */
  public static Optional<Price> parse(String raw) {
    if (ABSENT.equals(raw)) {
      return Optional.empty();
    }
    return Optional.of(new Price(Float.parseFloat(raw)));
  }

  public String label() {
    return String.format("%.0f CZK / 50g", amountPerGram * 50);
  }
}
