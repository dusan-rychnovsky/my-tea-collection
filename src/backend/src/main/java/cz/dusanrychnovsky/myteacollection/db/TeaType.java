package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "TeaTypes")
public class TeaType {

  /*
    CREATE TABLE myteacollection.TeaTypes (
      id SERIAL PRIMARY KEY,
      name VARCHAR(255) NOT NULL
    );

    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (1, 'Blend');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (2, 'White');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (3, 'Yellow');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (4, 'Green');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (5, 'Oolong');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (6, 'Black');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (7, 'Dark');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (8, 'Sheng Puerh');
    INSERT INTO myteacollection.TeaTypes (id, name) VALUES (9, 'Shu Puerh');
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  public TeaType(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public TeaType() {
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
