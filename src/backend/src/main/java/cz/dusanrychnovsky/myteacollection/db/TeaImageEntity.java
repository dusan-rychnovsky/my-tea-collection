package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

// https://stackoverflow.com/questions/1444227/how-can-i-make-a-jpa-onetoone-relation-lazy

@Entity
@Table(schema = "myteacollection", name = "TeaImages")
public class TeaImageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tea_id", nullable = false)
  private TeaEntity tea;

  @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private TeaImageDataEntity data;

  private Integer index;

  public TeaImageEntity(TeaEntity tea, Integer index, TeaImageDataEntity data) {
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
    return data.getBytes();
  }
}
