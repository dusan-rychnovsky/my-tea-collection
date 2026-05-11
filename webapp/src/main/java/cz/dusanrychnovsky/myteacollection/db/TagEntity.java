package cz.dusanrychnovsky.myteacollection.db;

import cz.dusanrychnovsky.myteacollection.db.users.UserEntity;
import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Tags")
public class TagEntity {

  public TagEntity(long id, UserEntity user, String label, String description) {
    this.id = id;
    this.user = user;
    this.label = label;
    this.description = description;
  }

  public TagEntity() {
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private UserEntity user;

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
