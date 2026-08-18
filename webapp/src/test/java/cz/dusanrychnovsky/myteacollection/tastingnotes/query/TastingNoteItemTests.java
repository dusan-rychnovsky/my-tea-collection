package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TastingNoteItemTests {

  @Test
  void from_derivesAuthorNameAndInitialsFromOwner() {
    var item = TastingNoteItem.from(note(user("Dušan", "Rychnovský", null), 10, "Great."));
    assertEquals("Dušan R.", item.authorName());
    assertEquals("DR", item.initials());
  }

  @Test
  void from_missingLastName_usesFirstNameOnly() {
    var item = TastingNoteItem.from(note(user("Dušan", null, null), 8, "Nice."));
    assertEquals("Dušan", item.authorName());
    assertEquals("D", item.initials());
  }

  @Test
  void from_onlyNickName_usedAsFallback() {
    var item = TastingNoteItem.from(note(user(null, null, "teaLover"), 8, "Nice."));
    assertEquals("teaLover", item.authorName());
    assertEquals("T", item.initials());
  }

  @Test
  void from_noNameInfo_fallsBackWithoutExposingEmail() {
    var item = TastingNoteItem.from(note(user(null, null, null), 8, "Nice."));
    assertEquals("Anonymous", item.authorName());
    assertEquals("?", item.initials());
  }

  @Test
  void from_ratingLabelIsOneDecimal() {
    assertEquals("4.5", TastingNoteItem.from(note(user("A", "B", null), 9, "x")).ratingLabel());
  }

  @Test
  void from_dateLabelIsEnglishDayMonthYear() {
    var note = new TastingNoteEntity(null, user("A", "B", null), 8, LocalDate.of(2026, 7, 21), "x");
    assertEquals("21 Jul 2026", TastingNoteItem.from(note).dateLabel());
  }

  @Test
  void from_avatarClassIsFixedPaletteEntry() {
    var owner = user("Dušan", "Rychnovský", null);
    assertEquals("avatar-sage", TastingNoteItem.from(note(owner, 8, "x")).avatarClass());
  }

  @Test
  void paragraphs_splitsOnBlankLines() {
    assertEquals(List.of("Para one.", "Para two."), TastingNoteItem.paragraphs("Para one.\n\nPara two."));
  }

  @Test
  void paragraphs_dropsStrayBlankLinesAndTrims() {
    assertEquals(List.of("One.", "Two."), TastingNoteItem.paragraphs("\n\n  One.  \n\n\n  Two.\n\n"));
  }

  @Test
  void paragraphs_handlesCrlf() {
    assertEquals(List.of("One.", "Two."), TastingNoteItem.paragraphs("One.\r\n\r\nTwo."));
  }

  @Test
  void paragraphs_singleParagraph() {
    assertEquals(List.of("Just one."), TastingNoteItem.paragraphs("Just one."));
  }

  private static TastingNoteEntity note(UserEntity user, int halfStars, String body) {
    return new TastingNoteEntity(null, user, halfStars, LocalDate.of(2026, 1, 1), body);
  }

  private static UserEntity user(String firstName, String lastName, String nickName) {
    return new UserEntity("owner@example.com", "pwd", firstName, lastName, nickName, null, null);
  }
}
