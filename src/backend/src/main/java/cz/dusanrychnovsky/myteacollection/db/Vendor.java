package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Vendors")
public class Vendor {

  /*
    CREATE TABLE myteacollection.Vendors (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) NOT NULL,
      url VARCHAR(255) NOT NULL
    );

    INSERT INTO Vendors (id, name, url) VALUES (1, 'Mei Leaf', 'https://meileaf.com');
    INSERT INTO Vendors (id, name, url) VALUES (2, 'Meetea', 'https://www.meetea.cz');
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String url;

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
