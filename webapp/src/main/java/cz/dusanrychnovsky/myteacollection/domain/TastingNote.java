package cz.dusanrychnovsky.myteacollection.domain;

import java.time.LocalDate;

/**
 * A tasting of one tea by one taster, with its rating, tasting date and free-text body.
 * Tea and taster are intrinsic references held by id; the application verifies their existence.
 */
public final class TastingNote {

  private final Long teaId;
  private final Long tasterId;
  private final Rating rating;
  private final LocalDate tastedOn;
  private final String body;

  public TastingNote(
    Long teaId, Long tasterId, Rating rating, LocalDate tastedOn, String body) {

    if (teaId == null) {
      throw new IllegalArgumentException("Tasting note must reference a tea.");
    }
    if (tasterId == null) {
      throw new IllegalArgumentException("Tasting note must reference a taster.");
    }
    if (rating == null) {
      throw new IllegalArgumentException("Tasting note rating must not be null.");
    }
    if (tastedOn == null) {
      throw new IllegalArgumentException("Tasting note date must not be null.");
    }
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Tasting note body must not be blank.");
    }

    this.teaId = teaId;
    this.tasterId = tasterId;
    this.rating = rating;
    this.tastedOn = tastedOn;
    this.body = body;
  }

  public Long getTeaId() {
    return teaId;
  }

  public Long getTasterId() {
    return tasterId;
  }

  public Rating getRating() {
    return rating;
  }

  public LocalDate getTastedOn() {
    return tastedOn;
  }

  public String getBody() {
    return body;
  }
}
