package cz.dusanrychnovsky.myteacollection.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<VendorEntity, Long> {
}
