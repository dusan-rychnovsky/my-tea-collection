package cz.dusanrychnovsky.myteacollection.domain;

import java.util.Locale;

/**
 * A tasting-note rating on a 0–5 star scale in half-star steps, stored as an integer number of
 * half-stars (0–10, so 9 means 4.5 stars). The value is validated on construction; the conversions
 * to the 0.0–5.0 display value and its label live here so the read and (future) write sides share
 * one definition.
 */
public record Rating(int halfStars) {

  public Rating {
    if (halfStars < 0 || halfStars > 10) {
      throw new IllegalArgumentException("Rating must be between 0 and 10 half-stars: " + halfStars);
    }
  }

  /**
   * The rating as a 0.0–5.0 value (e.g. 9 half-stars → 4.5).
   */
  public double value() {
    return halfStars / 2.0;
  }

  /**
   * The rating rounded half-up to a whole star (0–5), used for distribution bucketing.
   */
  public int roundedStars() {
    return (int) Math.round(value());
  }

  /**
   * A one-decimal label in English formatting (e.g. "4.5").
   */
  public String label() {
    return String.format(Locale.ENGLISH, "%.1f", value());
  }
}
