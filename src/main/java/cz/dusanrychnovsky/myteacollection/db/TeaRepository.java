package cz.dusanrychnovsky.myteacollection.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeaRepository extends JpaRepository<TeaEntity, Long> {

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE TeaEntity t SET t.inStock = :inStock WHERE t.id = :id")
  int updateInStock(@Param("id") Long id, @Param("inStock") boolean inStock);
}
