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

    INSERT INTO myteacollection.Vendors (id, name, url) VALUES (1, 'Mei Leaf', 'https://meileaf.com');
    INSERT INTO myteacollection.Vendors (id, name, url) VALUES (2, 'Meetea', 'https://www.meetea.cz');
    INSERT INTO myteacollection.Vendors (id, name, url) VALUES (3, 'Chuť Čaje', 'https://www.chutcaje.cz/');
    INSERT INTO myteacollection.Vendors (id, name, url) VALUES (4, 'Lao Tea', 'https://www.laoteashop.cz/');
    INSERT INTO myteacollection.Vendors (id, name, url) VALUES (5, 'Klasek Tea', 'https://www.darjeeling.cz/');
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String url;

  public Vendor(Long id, String name, String url) {
    this.id = id;
    this.name = name;
    this.url = url;
  }

  public Vendor() {
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
