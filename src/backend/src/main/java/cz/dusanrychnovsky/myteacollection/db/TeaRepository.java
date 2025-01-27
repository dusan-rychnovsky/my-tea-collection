package cz.dusanrychnovsky.myteacollection.db;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeaRepository extends JpaRepository<Tea, Long> {
}
