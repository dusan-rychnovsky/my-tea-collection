package cz.dusanrychnovsky.myteacollection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeaTypeRepository extends JpaRepository<TeaTypeEntity, Long> {
}
