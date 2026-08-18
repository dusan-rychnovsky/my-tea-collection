package cz.dusanrychnovsky.myteacollection.tastingnotes.query;

import cz.dusanrychnovsky.myteacollection.domain.Rating;
import cz.dusanrychnovsky.myteacollection.persistence.TastingNoteEntity;
import cz.dusanrychnovsky.myteacollection.persistence.users.UserEntity;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Read model for a single tasting note as shown on the tea detail page. The displayed author
 * (name, initials, avatar colour) is derived from the note's owning user — there is no separate
 * author field. The body, stored as one plain-text column, is split on blank lines into ordered
 * paragraphs (trimmed, empties dropped) so each renders as its own {@code <p>}.
 */
public record TastingNoteItem(
  String authorName,
  String initials,
  String avatarClass,
  String ratingLabel,
  String dateLabel,
  List<String> paragraphs
) {

  private static final DateTimeFormatter DATE_FORMAT =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

  // A single palette colour for now; per-user avatar colours are deferred until tasting notes can
  // have distinct owners (see TASTING_NOTES_PLAN.md).
  private static final String AVATAR_CLASS = "avatar-sage";

  public static TastingNoteItem from(TastingNoteEntity note) {
    var author = note.getUser();
    return new TastingNoteItem(
      authorName(author),
      initials(author),
      AVATAR_CLASS,
      new Rating(note.getRatingHalfStars()).label(),
      DATE_FORMAT.format(note.getTastedOn()),
      paragraphs(note.getBody())
    );
  }

  static List<String> paragraphs(String body) {
    return Arrays.stream(body.replace("\r\n", "\n").replace("\r", "\n").split("\n\\s*\n"))
      .map(String::trim)
      .filter(paragraph -> !paragraph.isEmpty())
      .toList();
  }

  private static String authorName(UserEntity user) {
    var first = trimToNull(user.getFirstName());
    var last = trimToNull(user.getLastName());
    if (first != null && last != null) {
      return first + " " + firstLetter(last) + ".";
    }
    if (first != null) {
      return first;
    }
    var nick = trimToNull(user.getNickName());
    return nick != null ? nick : "Anonymous";
  }

  private static String initials(UserEntity user) {
    var first = trimToNull(user.getFirstName());
    var last = trimToNull(user.getLastName());
    if (first != null && last != null) {
      return firstLetter(first) + firstLetter(last);
    }
    if (first != null) {
      return firstLetter(first);
    }
    var nick = trimToNull(user.getNickName());
    return nick != null ? firstLetter(nick) : "?";
  }

  private static String firstLetter(String value) {
    return value.substring(0, 1).toUpperCase(Locale.ROOT);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    var trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
