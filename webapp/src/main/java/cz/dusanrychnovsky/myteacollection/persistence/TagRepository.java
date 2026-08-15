package cz.dusanrychnovsky.myteacollection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<TagEntity, Long> {
}
