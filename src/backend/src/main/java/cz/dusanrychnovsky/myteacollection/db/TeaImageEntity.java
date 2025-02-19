package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "TeaImages")
public class TeaImageEntity {

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

  @ManyToOne
  @JoinColumn(name = "tea_id", nullable = false)
  private TeaEntity tea;

  private Integer index;

  @Column(columnDefinition = "BYTEA")
  private byte[] data;

  public TeaImageEntity(TeaEntity tea, Integer index, byte[] data) {
    this.tea = tea;
    this.index = index;
    this.data = data;
  }

  public TeaImageEntity() {
  }

  public Long getId() {
    return id;
  }

  public TeaEntity getTea() {
    return tea;
  }

  public Integer getIndex() {
    return index;
  }

  public TeaImageEntity setIndex(Integer index) {
    this.index = index;
    return this;
  }

  public byte[] getData() {
    return data;
  }
}
