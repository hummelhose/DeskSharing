package io.github.hummelhose.desksharing.infrastructure.persistence.repository;

import io.github.hummelhose.desksharing.domain.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByActiveTrue();

    List<Room> findByOfficeId(Long officeId);

    List<Room> findByOfficeIdAndActiveTrue(Long officeId);

    List<Room> findByOfficeIsNull();

    boolean existsByOfficeId(Long officeId);
}