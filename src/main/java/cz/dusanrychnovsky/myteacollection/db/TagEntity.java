package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Tags")
public class TagEntity {

  public TagEntity(long id, String label, String description) {
    this.id = id;
    this.label = label;
    this.description = description;
  }

  public TagEntity() {
  }

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
