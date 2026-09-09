package cz.dusanrychnovsky.myteacollection.domain;

import java.util.Optional;

/**
 * The SCOPE of a tea (Season, Cultivar, Origin, Elevation) as a write-side value object.
 * Season absence is represented by an empty optional; the remaining fields may be absent (null).
 */
public record TeaScope(Optional<Season> season, String cultivar, String origin, String elevation) {

	public TeaScope {
		if (season == null) {
			throw new IllegalArgumentException("Tea season must not be null.");
		}
	}
}
