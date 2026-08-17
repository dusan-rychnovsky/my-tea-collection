package cz.dusanrychnovsky.myteacollection.domain;

import java.time.LocalDate;

/**
 * Write-side aggregate for a single tasting note: its rating, the date it was tasted and its
 * free-text body (paragraphs separated by blank lines). Like {@link Tea}, it deliberately does not
 * hold its tea or its owner — those are context supplied by the {@code ReplaceTeaTastingNotes}
 * application service (a tasting note is always authored as part of replacing one tea's notes), not
 * properties of the note itself.
 *
 * <p>The constructor enforces every invariant checkable from the note's own state, throwing
 * {@link IllegalArgumentException} on violation: {@code rating} and {@code tastedOn} must be
 * non-null and {@code body} must not be blank (i.e. at least one non-blank paragraph). The rating's
 * range/step invariant is guarded by the {@link Rating} value object.
 */
public final class TastingNote {

  private final Rating rating;
  private final LocalDate tastedOn;
  private final String body;

  public TastingNote(Rating rating, LocalDate tastedOn, String body) {
    if (rating == null) {
      throw new IllegalArgumentException("Tasting note rating must not be null.");
    }
    if (tastedOn == null) {
      throw new IllegalArgumentException("Tasting note date must not be null.");
    }
    if (body == null || body.isBlank()) {
      throw new IllegalArgumentException("Tasting note body must not be blank.");
    }

    this.rating = rating;
    this.tastedOn = tastedOn;
    this.body = body;
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
