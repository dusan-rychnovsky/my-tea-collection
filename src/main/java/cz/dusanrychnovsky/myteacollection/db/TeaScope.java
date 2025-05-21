package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.Embeddable;

@Embeddable
public class TeaScope {

  private String season;

  private String cultivar;

  private String origin;

  private String elevation;

  public TeaScope(String season, String cultivar, String origin, String elevation) {
    this.season = season;
    this.cultivar = cultivar;
    this.origin = origin;
    this.elevation = elevation;
  }

  public TeaScope() {
  }

  public String getElevation() {
    return elevation;
  }

  public String getOrigin() {
    return origin;
  }

  public String getCultivar() {
    return cultivar;
  }

  public String getSeason() {
    return season;
  }
}
