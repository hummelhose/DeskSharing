package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.OfficeRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final OfficeRepository officeRepository;

    public RoomService(RoomRepository roomRepository,
                       OfficeRepository officeRepository) {
        this.roomRepository = roomRepository;
        this.officeRepository = officeRepository;
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> getAllActiveRooms() {
        return roomRepository.findByActiveTrue();
    }

    public List<Room> getRoomsByOfficeId(Long officeId) {
        return roomRepository.findByOfficeId(officeId);
    }

    public List<Room> getActiveRoomsByOfficeId(Long officeId) {
        return roomRepository.findByOfficeIdAndActiveTrue(officeId);
    }

    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    public Room createRoom(String name,
                           String description,
                           boolean active,
                           Integer layoutWidth,
                           Integer layoutHeight) {

        Room room = new Room(
                name,
                description,
                active,
                layoutWidth,
                layoutHeight
        );

        return roomRepository.save(room);
    }

    public Optional<Room> createRoom(Long officeId,
                                     String name,
                                     String description,
                                     boolean active,
                                     Integer posX,
                                     Integer posY,
                                     Integer layoutWidth,
                                     Integer layoutHeight) {

        Optional<Office> officeOptional =
                officeRepository.findById(officeId);

        if (officeOptional.isEmpty()) {
            return Optional.empty();
        }

        Room room = new Room(
                officeOptional.get(),
                name,
                description,
                active,
                posX,
                posY,
                layoutWidth,
                layoutHeight
        );

        return Optional.of(roomRepository.save(room));
    }

    public Optional<Room> updateRoom(Long id,
                                     String name,
                                     String description,
                                     boolean active,
                                     Integer layoutWidth,
                                     Integer layoutHeight) {

        return roomRepository.findById(id)
                .map(existingRoom -> {
                    existingRoom.setName(name);
                    existingRoom.setDescription(description);
                    existingRoom.setActive(active);
                    existingRoom.setLayoutWidth(layoutWidth);
                    existingRoom.setLayoutHeight(layoutHeight);

                    return roomRepository.save(existingRoom);
                });
    }

    public Optional<Room> updateRoom(Long id,
                                     Long officeId,
                                     String name,
                                     String description,
                                     boolean active,
                                     Integer posX,
                                     Integer posY,
                                     Integer layoutWidth,
                                     Integer layoutHeight) {

        Optional<Room> roomOptional =
                roomRepository.findById(id);

        Optional<Office> officeOptional =
                officeRepository.findById(officeId);

        if (roomOptional.isEmpty() || officeOptional.isEmpty()) {
            return Optional.empty();
        }

        Room existingRoom = roomOptional.get();

        existingRoom.setOffice(officeOptional.get());
        existingRoom.setName(name);
        existingRoom.setDescription(description);
        existingRoom.setActive(active);
        existingRoom.setPosX(Math.max(posX, 0));
        existingRoom.setPosY(Math.max(posY, 0));
        existingRoom.setLayoutWidth(Math.max(layoutWidth, 100));
        existingRoom.setLayoutHeight(Math.max(layoutHeight, 100));

        return Optional.of(roomRepository.save(existingRoom));
    }

    public Optional<Room> updateRoomPosition(Long roomId,
                                             int posX,
                                             int posY) {

        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setPosX(Math.max(posX, 0));
                    room.setPosY(Math.max(posY, 0));

                    return roomRepository.save(room);
                });
    }

    public Optional<Room> updateRoomSize(Long roomId,
                                         int layoutWidth,
                                         int layoutHeight) {

        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setLayoutWidth(Math.max(layoutWidth, 100));
                    room.setLayoutHeight(Math.max(layoutHeight, 100));

                    return roomRepository.save(room);
                });
    }

    public Optional<Room> updateRoomLayout(Long roomId,
                                           int posX,
                                           int posY,
                                           int layoutWidth,
                                           int layoutHeight) {

        return roomRepository.findById(roomId)
                .map(room -> {
                    room.setPosX(Math.max(posX, 0));
                    room.setPosY(Math.max(posY, 0));
                    room.setLayoutWidth(Math.max(layoutWidth, 100));
                    room.setLayoutHeight(Math.max(layoutHeight, 100));

                    return roomRepository.save(room);
                });
    }

    public Optional<Room> assignRoomToOffice(Long roomId,
                                             Long officeId) {

        Optional<Room> roomOptional =
                roomRepository.findById(roomId);

        Optional<Office> officeOptional =
                officeRepository.findById(officeId);

        if (roomOptional.isEmpty() || officeOptional.isEmpty()) {
            return Optional.empty();
        }

        Room room = roomOptional.get();
        room.setOffice(officeOptional.get());

        if (room.getPosX() == null) {
            room.setPosX(0);
        }

        if (room.getPosY() == null) {
            room.setPosY(0);
        }

        return Optional.of(roomRepository.save(room));
    }

    @Transactional
    public boolean deleteRoom(Long roomId) {
        Optional<Room> roomOptional =
                roomRepository.findById(roomId);

        if (roomOptional.isEmpty()) {
            return false;
        }

        roomRepository.delete(roomOptional.get());
        return true;
    }
}