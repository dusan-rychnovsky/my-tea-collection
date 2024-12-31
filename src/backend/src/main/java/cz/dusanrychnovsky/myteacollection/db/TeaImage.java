package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "TeaImages")
public class TeaImage {

  /*
    CREATE TABLE myteacollection.TeaImages (
      id SERIAL PRIMARY KEY,
      tea_id BIGINT NOT NULL,
      index INT,
      data BYTEA,
      FOREIGN KEY (tea_id) REFERENCES myteacollection.Teas(id)
    );
   */

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Integer index;

  private byte[] data;

  public Long getId() {
    return id;
  }

  public Integer getIndex() {
    return index;
  }

  public TeaImage setIndex(Integer index) {
    this.index = index;
    return this;
  }

  public byte[] getData() {
    return data;
  }
}
