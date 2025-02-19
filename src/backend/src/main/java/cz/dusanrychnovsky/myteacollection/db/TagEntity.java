package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Tags")
public class TagEntity {

  /*
    CREATE TABLE myteacollection.Tags (
      id SERIAL PRIMARY KEY,
      label VARCHAR(255) NOT NULL,
      description TEXT
    );
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String label;

  private String description;

  public Long getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public String getDescription() {
    return description;
  }

}
