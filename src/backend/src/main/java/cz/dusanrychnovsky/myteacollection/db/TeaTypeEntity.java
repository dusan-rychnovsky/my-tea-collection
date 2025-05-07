package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "TeaTypes")
public class TeaTypeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  public TeaTypeEntity(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public TeaTypeEntity() {
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
