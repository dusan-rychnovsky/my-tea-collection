package cz.dusanrychnovsky.myteacollection.domain;

/**
 * The SCOPE of a tea (Season, Cultivar, Origin, Elevation) as a write-side value object.
 * Any field may be absent (null).
 */
public record TeaScope(String season, String cultivar, String origin, String elevation) {
}
