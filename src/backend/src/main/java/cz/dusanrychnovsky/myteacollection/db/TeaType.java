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

    INSERT INTO TeaTypes (id, name) VALUES (1, 'Blend');
    INSERT INTO TeaTypes (id, name) VALUES (2, 'White');
    INSERT INTO TeaTypes (id, name) VALUES (3, 'Yellow');
    INSERT INTO TeaTypes (id, name) VALUES (4, 'Green');
    INSERT INTO TeaTypes (id, name) VALUES (5, 'Oolong');
    INSERT INTO TeaTypes (id, name) VALUES (6, 'Black');
    INSERT INTO TeaTypes (id, name) VALUES (7, 'Dark');
    INSERT INTO TeaTypes (id, name) VALUES (8, 'Sheng Puerh');
    INSERT INTO TeaTypes (id, name) VALUES (9, 'Shu Puerh');
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
