package io.github.hummelhose.desksharing.infrastructure.config;

import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.OfficeRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.RoomRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeskSharingDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_OFFICE_NAME = "Büro Duisburg";

    private final OfficeRepository officeRepository;
    private final RoomRepository roomRepository;

    public DeskSharingDataInitializer(OfficeRepository officeRepository,
                                      RoomRepository roomRepository) {
        this.officeRepository = officeRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Office defaultOffice = officeRepository.findFirstByNameOrderByIdAsc(DEFAULT_OFFICE_NAME)
                .orElseGet(() -> officeRepository.save(new Office(
                        DEFAULT_OFFICE_NAME,
                        "Standard-Büro für bestehende Räume.",
                        true,
                        1200,
                        700
                )));

        List<Room> roomsWithoutOffice = roomRepository.findByOfficeIsNull();

        for (Room room : roomsWithoutOffice) {
            room.setOffice(defaultOffice);

            if (room.getPosX() == null) {
                room.setPosX(20);
            }

            if (room.getPosY() == null) {
                room.setPosY(20);
            }

            roomRepository.save(room);
        }
    }
}