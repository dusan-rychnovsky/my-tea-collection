package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Vendors")
public class VendorEntity {

  /*
    CREATE TABLE myteacollection.Vendors (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      url VARCHAR(255) NOT NULL
    );
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String url;

  public VendorEntity(Long id, String name, String url) {
    this.id = id;
    this.name = name;
    this.url = url;
  }

  public VendorEntity() {
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }
}
