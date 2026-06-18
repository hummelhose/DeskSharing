package io.github.hummelhose.desksharing.infrastructure.persistence.repository;

import io.github.hummelhose.desksharing.domain.model.Office;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficeRepository extends JpaRepository<Office, Long> {

    List<Office> findByActiveTrue();

    Optional<Office> findFirstByNameOrderByIdAsc(String name);
}