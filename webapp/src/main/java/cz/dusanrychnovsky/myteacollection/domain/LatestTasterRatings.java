package cz.dusanrychnovsky.myteacollection.domain;

import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

/**
 * One current rating per taster for a single tea's tasting notes. A later tasting date supersedes
 * an earlier one; same-date ties arbitrarily keep the first note encountered.
 */
public record LatestTasterRatings(List<Rating> ratings) {

  public LatestTasterRatings {
    if (ratings == null) {
      throw new IllegalArgumentException("Latest taster ratings must not be null.");
    }
    ratings = List.copyOf(ratings);
  }

  public int tasterCount() {
    return ratings.size();
  }

  public OptionalDouble averageHalfStars() {
    return ratings.stream()
      .mapToInt(Rating::halfStars)
      .average();
  }

  public static LatestTasterRatings of(List<TastingNote> notes) {
    if (notes == null) {
      throw new IllegalArgumentException("Tasting notes must not be null.");
    }

    var latestByTaster = new HashMap<Long, TastingNote>();
    Long teaId = null;
    for (var note : notes) {
      if (note == null) {
        throw new IllegalArgumentException("Tasting notes must not contain null.");
      }
      if (teaId != null && !teaId.equals(note.getTeaId())) {
        throw new IllegalArgumentException("Tasting notes must belong to the same tea.");
      }
      teaId = note.getTeaId();

      latestByTaster.merge(
        note.getTasterId(),
        note,
        (current, candidate) -> candidate.getTastedOn().isAfter(current.getTastedOn())
          ? candidate : current
      );
    }

    return new LatestTasterRatings(latestByTaster.values().stream()
      .map(TastingNote::getRating)
      .toList());
  }
}
