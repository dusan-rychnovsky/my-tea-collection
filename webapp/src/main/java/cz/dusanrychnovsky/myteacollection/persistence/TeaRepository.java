package cz.dusanrychnovsky.myteacollection.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeaRepository extends JpaRepository<TeaEntity, Long> {

  Optional<TeaEntity> findBySlug(String slug);

  boolean existsBySlug(String slug);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE TeaEntity t SET t.inStock = :inStock WHERE t.id = :id")
  int updateInStock(@Param("id") Long id, @Param("inStock") boolean inStock);

  @Query("SELECT t.id AS id, t.inStock AS inStock FROM TeaEntity t")
  List<TeaAvailability> findAllAvailability();

  interface TeaAvailability {
    Long getId();
    boolean isInStock();
  }
}
