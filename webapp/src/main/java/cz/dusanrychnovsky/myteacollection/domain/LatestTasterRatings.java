package cz.dusanrychnovsky.myteacollection.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalDouble;

/**
 * One current rating per taster. A later tasting date supersedes an earlier one; when dates match,
 * the rating recorded later wins.
 */
public record LatestTasterRatings(List<Rating> ratings) {

  public record RatedTasting(
    long tasterId,
    long recordedOrder,
    LocalDate tastedOn,
    Rating rating
  ) {
    public RatedTasting {
      if (tastedOn == null) {
        throw new IllegalArgumentException("Rated tasting date must not be null.");
      }
      if (rating == null) {
        throw new IllegalArgumentException("Rated tasting rating must not be null.");
      }
    }
  }

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

  public static LatestTasterRatings of(List<RatedTasting> tastings) {
    if (tastings == null) {
      throw new IllegalArgumentException("Rated tastings must not be null.");
    }

    var latestFirst = Comparator
      .comparing(RatedTasting::tastedOn)
      .thenComparingLong(RatedTasting::recordedOrder)
      .reversed();
    var latestByTaster = new HashMap<Long, RatedTasting>();
    for (var tasting : tastings) {
      latestByTaster.merge(
        tasting.tasterId(),
        tasting,
        (current, candidate) -> latestFirst.compare(current, candidate) <= 0 ? current : candidate
      );
    }

    return new LatestTasterRatings(latestByTaster.values().stream()
      .map(RatedTasting::rating)
      .toList());
  }
}
