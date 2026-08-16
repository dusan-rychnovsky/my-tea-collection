package cz.dusanrychnovsky.myteacollection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TastingNoteRepository extends JpaRepository<TastingNoteEntity, Long> {

  /**
   * A tea's tasting notes, newest first with the note id as a stable tie-break. The owning user is
   * fetched in the same query so the read model can derive the author without a lazy load per note.
   */
  @Query("""
    select note from TastingNoteEntity note
    join fetch note.user
    where note.tea.id = :teaId
    order by note.tastedOn desc, note.id desc
    """)
  List<TastingNoteEntity> findByTeaIdNewestFirst(@Param("teaId") Long teaId);
}
