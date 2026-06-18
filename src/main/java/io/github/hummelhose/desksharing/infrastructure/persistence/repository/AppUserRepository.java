package io.github.hummelhose.desksharing.infrastructure.persistence.repository;

import io.github.hummelhose.desksharing.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEntraOid(String entraOid);

    Optional<AppUser> findByEmail(String email);
}