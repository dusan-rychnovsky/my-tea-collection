package cz.dusanrychnovsky.myteacollection.db.users;

import jakarta.persistence.*;

@Entity
@Table(schema = "myteacollection", name = "Users")
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  private String password;

  private String firstName;

  private String lastName;

  private String nickName;

  private String location;

  private String aboutMe;

  public UserEntity(String email, String password, String firstName, String lastName, String nickName, String location, String aboutMe) {
    this.email = email;
    this.password = password;
    this.firstName = firstName;
    this.lastName = lastName;
    this.nickName = nickName;
    this.location = location;
    this.aboutMe = aboutMe;
  }

  public UserEntity() {
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getNickName() {
    return nickName;
  }

  public String getLocation() {
    return location;
  }

  public String getAboutMe() {
    return aboutMe;
  }
}
