package cz.dusanrychnovsky.myteacollection.persistence;

import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(schema = "myteacollection", name = "TastingNotes")
public class TastingNoteEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "tea_id", nullable = false)
  private TeaEntity tea;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "rating_half_stars", nullable = false)
  private int ratingHalfStars;

  @Column(name = "tasted_on", nullable = false)
  private LocalDate tastedOn;

  @Column(name = "body", columnDefinition = "TEXT", nullable = false)
  private String body;

  public TastingNoteEntity(
    TeaEntity tea, UserEntity user, int ratingHalfStars, LocalDate tastedOn, String body) {

    this.tea = tea;
    this.user = user;
    this.ratingHalfStars = ratingHalfStars;
    this.tastedOn = tastedOn;
    this.body = body;
  }

  public TastingNoteEntity() {
  }

  public Long getId() {
    return id;
  }

  public TeaEntity getTea() {
    return tea;
  }

  public UserEntity getUser() {
    return user;
  }

  public int getRatingHalfStars() {
    return ratingHalfStars;
  }

  public LocalDate getTastedOn() {
    return tastedOn;
  }

  public String getBody() {
    return body;
  }
}
