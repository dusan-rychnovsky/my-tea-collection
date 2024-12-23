package cz.dusanrychnovsky.myteacollection;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "test")
public class Test {

  @Id
  @Column(name = "id")
  private Long id;

  @Column(name = "test_col")
  private String test;

  public String getTest() {
    return test;
  }

  public void setTest(String test) {
    this.test = test;
  }
}
