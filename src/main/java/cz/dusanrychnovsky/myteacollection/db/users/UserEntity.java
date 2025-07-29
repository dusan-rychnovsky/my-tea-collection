package cz.dusanrychnovsky.myteacollection.db.users;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String firstName;

  private String lastName;

  private String password;

  private String email;

  public UserEntity(String email, String password, String firstName, String lastName) {
    this.email = email;
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public Long getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getPassword() {
    return password;
  }

  public String getEmail() {
    return email;
  }
}
