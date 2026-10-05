package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Read model aggregating a tea's tasting notes: the count, the average of each taster's latest
 * rating and the per-star distribution shown above the notes list. Computed from the notes on the
 * read side (nothing is stored). The distribution has six rows, 5★ down to 0★; each note rounds
 * half-up into exactly one bucket, so the bar counts always sum to {@link #count()}. Absence of
 * notes is represented by {@link #hasNotes()} being {@code false} and a {@code null}
 * {@link #averageLabel()} — never a 0.0 average, which is a valid rating.
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

  public static RatingSummary of(List<TastingNoteEntity> notes) {
    var count = notes.size();
    var countLabel = count + (count == 1 ? " tasting note" : " tasting notes");
    if (count == 0) {
      return new RatingSummary(0, countLabel, false, null, "0 tasters", List.of());
    }

    var ratings = notes.stream().map(note -> new Rating(note.getRatingHalfStars())).toList();
    var latestRatings = latestRatingsByUser(notes);
    var sumHalfStars = latestRatings.stream().mapToInt(Rating::halfStars).sum();
    var average = sumHalfStars / 2.0 / latestRatings.size();
    var averageLabel = String.format(Locale.ENGLISH, "%.1f", average);
    var averageCountLabel = latestRatings.size() + (latestRatings.size() == 1 ? " taster" : " tasters");

    return new RatingSummary(
      count, countLabel, true, averageLabel, averageCountLabel, distribution(ratings, count));
  }

  private static List<Rating> latestRatingsByUser(List<TastingNoteEntity> notes) {
    var newestFirst = Comparator
      .comparing(TastingNoteEntity::getTastedOn)
      .thenComparing(TastingNoteEntity::getId)
      .reversed();
    var latestByUser = new HashMap<Long, TastingNoteEntity>();
    for (var note : notes) {
      latestByUser.merge(note.getUser().getId(), note,
        (current, candidate) -> newestFirst.compare(current, candidate) <= 0 ? current : candidate);
    }
    return latestByUser.values().stream()
      .map(note -> new Rating(note.getRatingHalfStars()))
      .toList();
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
