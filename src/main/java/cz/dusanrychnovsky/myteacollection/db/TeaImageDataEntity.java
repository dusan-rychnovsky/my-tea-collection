package cz.dusanrychnovsky.myteacollection.db;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "TeaImageData")
public class TeaImageDataEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "BYTEA")
  @Basic(optional = false, fetch = FetchType.LAZY)
  private byte[] bytes;

  public TeaImageDataEntity(byte[] bytes) {
    this.bytes = bytes;
  }

  public TeaImageDataEntity() {
  }

  public byte[] getBytes() {
    return bytes;
  }
}
