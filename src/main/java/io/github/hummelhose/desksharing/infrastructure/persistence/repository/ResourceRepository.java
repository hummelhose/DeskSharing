package io.github.hummelhose.desksharing.infrastructure.persistence.repository;

import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByActiveTrue();

    List<Resource> findByRoomId(Long roomId);

    List<Resource> findByRoomIdAndActiveTrue(Long roomId);

    List<Resource> findByResourceTypeAndActiveTrue(ResourceType resourceType);

    boolean existsByRoomId(Long roomId);
}