package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.domain.LatestTasterRatings;
import cz.dusanrychnovsky.myteacollection.domain.LatestTasterRatings.RatedTasting;
import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Read model for the rating summary shown above a tea's complete tasting-note history. The average
 * label comes from the tea's materialized sorting score; taster count and distribution are derived
 * from one latest rating per taster via {@link LatestTasterRatings}. The six distribution rows run
 * from 5★ down to 0★ and represent current opinions, while {@link #count()} counts all historical
 * tasting notes. Absence of notes is represented by {@link #hasNotes()} being {@code false} and a
 * {@code null} {@link #averageLabel()} — never a 0.0 average, which is a valid rating.
 */
public record RatingSummary(
  int count,
  String countLabel,
  boolean hasNotes,
  String averageLabel,
  String averageCountLabel,
  List<DistributionRow> distribution
) {

  public record DistributionRow(int stars, int count, int pct) {
  }

  public static RatingSummary of(
    Double averageRatingHalfStars, List<TastingNoteEntity> notes) {

    if (notes == null) {
      throw new IllegalArgumentException("Tasting notes must not be null.");
    }

    var count = notes.size();
    var countLabel = count + (count == 1 ? " tasting note" : " tasting notes");
    if (count == 0) {
      if (averageRatingHalfStars != null) {
        throw new IllegalStateException("Tea without tasting notes must not have an average rating.");
      }
      return new RatingSummary(0, countLabel, false, null, "0 tasters", List.of());
    }

    if (averageRatingHalfStars == null) {
      throw new IllegalStateException("Tea with tasting notes must have an average rating.");
    }
    if (!Double.isFinite(averageRatingHalfStars)
      || averageRatingHalfStars < 0
      || averageRatingHalfStars > 10) {
      throw new IllegalStateException(
        "Materialized average rating must be between 0 and 10 half-stars: "
          + averageRatingHalfStars);
    }

    var latestRatings = LatestTasterRatings.of(notes.stream()
      .map(note -> new RatedTasting(
        note.getUser().getId(),
        note.getId(),
        note.getTastedOn(),
        new Rating(note.getRatingHalfStars())
      ))
      .toList());
    var averageLabel = String.format(Locale.ENGLISH, "%.1f", averageRatingHalfStars / 2.0);
    var averageCountLabel = latestRatings.tasterCount()
      + (latestRatings.tasterCount() == 1 ? " taster" : " tasters");

    return new RatingSummary(
      count,
      countLabel,
      true,
      averageLabel,
      averageCountLabel,
      distribution(latestRatings.ratings(), latestRatings.tasterCount())
    );
  }

  private static List<DistributionRow> distribution(List<Rating> ratings, int total) {
    var rows = new ArrayList<DistributionRow>();
    for (var stars = 5; stars >= 0; stars--) {
      var bucket = stars;
      var bucketCount = (int) ratings.stream().filter(r -> r.roundedStars() == bucket).count();
      var pct = (int) Math.round(bucketCount * 100.0 / total);
      rows.add(new DistributionRow(stars, bucketCount, pct));
    }
    return List.copyOf(rows);
  }
}
