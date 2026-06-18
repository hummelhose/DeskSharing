package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ReservationRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ResourceRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ResourceService(ResourceRepository resourceRepository,
                           RoomRepository roomRepository,
                           ReservationRepository reservationRepository) {
        this.resourceRepository = resourceRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    public List<Resource> getAllActiveResources() {
        return resourceRepository.findByActiveTrue();
    }

    public List<Resource> getResourcesByRoomId(Long roomId) {
        return resourceRepository.findByRoomIdAndActiveTrue(roomId);
    }

    public List<Resource> getResourcesByType(ResourceType resourceType) {
        return resourceRepository.findByResourceTypeAndActiveTrue(resourceType);
    }

    public Optional<Resource> getResourceById(Long id) {
        return resourceRepository.findById(id);
    }

    public Optional<Resource> createResource(String name,
                                             String description,
                                             ResourceType resourceType,
                                             Long roomId,
                                             boolean active,
                                             boolean bookable,
                                             Integer posX,
                                             Integer posY) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    Resource resource = new Resource(
                            name,
                            description,
                            resourceType,
                            room,
                            active,
                            bookable,
                            posX,
                            posY
                    );

                    return resourceRepository.save(resource);
                });
    }

    public Optional<Resource> createResource(String name,
                                             String description,
                                             ResourceType resourceType,
                                             Long roomId,
                                             boolean active,
                                             boolean bookable,
                                             Integer posX,
                                             Integer posY,
                                             Integer width,
                                             Integer height) {
        return roomRepository.findById(roomId)
                .map(room -> {
                    Resource resource = new Resource(
                            name,
                            description,
                            resourceType,
                            room,
                            active,
                            bookable,
                            posX,
                            posY,
                            width,
                            height
                    );

                    return resourceRepository.save(resource);
                });
    }

    public Optional<Resource> updateResource(Long id,
                                             String name,
                                             String description,
                                             ResourceType resourceType,
                                             Long roomId,
                                             boolean active,
                                             boolean bookable,
                                             Integer posX,
                                             Integer posY) {
        Optional<Resource> resourceOptional = resourceRepository.findById(id);
        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if (resourceOptional.isEmpty() || roomOptional.isEmpty()) {
            return Optional.empty();
        }

        Resource existingResource = resourceOptional.get();
        Room room = roomOptional.get();

        existingResource.setName(name);
        existingResource.setDescription(description);
        existingResource.setResourceType(resourceType);
        existingResource.setRoom(room);
        existingResource.setActive(active);
        existingResource.setBookable(bookable);
        existingResource.setPosX(posX);
        existingResource.setPosY(posY);

        if (existingResource.getWidth() == null) {
            existingResource.setWidth(108);
        }

        if (existingResource.getHeight() == null) {
            existingResource.setHeight(74);
        }

        return Optional.of(resourceRepository.save(existingResource));
    }

    public Optional<Resource> updateResource(Long id,
                                             String name,
                                             String description,
                                             ResourceType resourceType,
                                             Long roomId,
                                             boolean active,
                                             boolean bookable,
                                             Integer posX,
                                             Integer posY,
                                             Integer width,
                                             Integer height) {
        Optional<Resource> resourceOptional = resourceRepository.findById(id);
        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if (resourceOptional.isEmpty() || roomOptional.isEmpty()) {
            return Optional.empty();
        }

        Resource existingResource = resourceOptional.get();
        Room room = roomOptional.get();

        existingResource.setName(name);
        existingResource.setDescription(description);
        existingResource.setResourceType(resourceType);
        existingResource.setRoom(room);
        existingResource.setActive(active);
        existingResource.setBookable(bookable);
        existingResource.setPosX(Math.max(posX, 0));
        existingResource.setPosY(Math.max(posY, 0));
        existingResource.setWidth(Math.max(width, 60));
        existingResource.setHeight(Math.max(height, 40));

        return Optional.of(resourceRepository.save(existingResource));
    }

    public Optional<Resource> updateResourcePosition(Long resourceId, int posX, int posY) {
        return resourceRepository.findById(resourceId)
                .map(resource -> {
                    resource.setPosX(Math.max(posX, 0));
                    resource.setPosY(Math.max(posY, 0));

                    if (resource.getWidth() == null) {
                        resource.setWidth(108);
                    }

                    if (resource.getHeight() == null) {
                        resource.setHeight(74);
                    }

                    return resourceRepository.save(resource);
                });
    }

    public Optional<Resource> updateResourceSize(Long resourceId, int width, int height) {
        return resourceRepository.findById(resourceId)
                .map(resource -> {
                    resource.setWidth(Math.max(width, 60));
                    resource.setHeight(Math.max(height, 40));

                    return resourceRepository.save(resource);
                });
    }

    public Optional<Resource> updateResourceLayout(Long resourceId,
                                                   int posX,
                                                   int posY,
                                                   int width,
                                                   int height) {
        return resourceRepository.findById(resourceId)
                .map(resource -> {
                    resource.setPosX(Math.max(posX, 0));
                    resource.setPosY(Math.max(posY, 0));
                    resource.setWidth(Math.max(width, 60));
                    resource.setHeight(Math.max(height, 40));

                    return resourceRepository.save(resource);
                });
    }

    public boolean deleteResource(Long resourceId) {
        if (resourceRepository.findById(resourceId).isEmpty()) {
            return false;
        }

        reservationRepository.findByResourceId(resourceId)
                .forEach(reservationRepository::delete);

        resourceRepository.deleteById(resourceId);
        return true;
    }
}