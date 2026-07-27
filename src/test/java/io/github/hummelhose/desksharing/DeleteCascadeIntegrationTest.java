package io.github.hummelhose.desksharing;

import io.github.hummelhose.desksharing.application.service.OfficeService;
import io.github.hummelhose.desksharing.application.service.ResourceService;
import io.github.hummelhose.desksharing.application.service.RoomService;
import io.github.hummelhose.desksharing.domain.model.AppRole;
import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.domain.model.Office;
import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.domain.model.ResourceType;
import io.github.hummelhose.desksharing.domain.model.Room;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.OfficeRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ReservationRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ResourceRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/migration"
        },
        showSql = false
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ActiveProfiles("test")
@Import({
        TestcontainersConfiguration.class,
        OfficeService.class,
        RoomService.class,
        ResourceService.class
})
class DeleteCascadeIntegrationTest {

    private final EntityManager entityManager;

    private final AppUserRepository appUserRepository;
    private final OfficeRepository officeRepository;
    private final RoomRepository roomRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;

    private final OfficeService officeService;
    private final RoomService roomService;
    private final ResourceService resourceService;

    @Autowired
    DeleteCascadeIntegrationTest(
            EntityManager entityManager,
            AppUserRepository appUserRepository,
            OfficeRepository officeRepository,
            RoomRepository roomRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            OfficeService officeService,
            RoomService roomService,
            ResourceService resourceService
    ) {
        this.entityManager = entityManager;
        this.appUserRepository = appUserRepository;
        this.officeRepository = officeRepository;
        this.roomRepository = roomRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.officeService = officeService;
        this.roomService = roomService;
        this.resourceService = resourceService;
    }

    @Test
    void deleteOffice_shouldDeleteRoomsResourcesAndReservations() {
        TestHierarchy hierarchy = createHierarchy();

        boolean deleted = officeService.deleteOffice(
                hierarchy.officeId()
        );

        flushAndClear();

        assertTrue(deleted);

        assertFalse(
                officeRepository.existsById(hierarchy.officeId())
        );
        assertFalse(
                roomRepository.existsById(hierarchy.roomId())
        );
        assertFalse(
                resourceRepository.existsById(hierarchy.resourceId())
        );
        assertFalse(
                reservationRepository.existsById(
                        hierarchy.reservationId()
                )
        );

        assertTrue(
                appUserRepository.existsById(hierarchy.appUserId())
        );
    }

    @Test
    void deleteRoom_shouldDeleteResourcesAndReservationsButKeepOffice() {
        TestHierarchy hierarchy = createHierarchy();

        boolean deleted = roomService.deleteRoom(
                hierarchy.roomId()
        );

        flushAndClear();

        assertTrue(deleted);

        assertTrue(
                officeRepository.existsById(hierarchy.officeId())
        );
        assertFalse(
                roomRepository.existsById(hierarchy.roomId())
        );
        assertFalse(
                resourceRepository.existsById(hierarchy.resourceId())
        );
        assertFalse(
                reservationRepository.existsById(
                        hierarchy.reservationId()
                )
        );
        assertTrue(
                appUserRepository.existsById(hierarchy.appUserId())
        );
    }

    @Test
    void deleteResource_shouldDeleteReservationsButKeepRoomAndOffice() {
        TestHierarchy hierarchy = createHierarchy();

        boolean deleted = resourceService.deleteResource(
                hierarchy.resourceId()
        );

        flushAndClear();

        assertTrue(deleted);

        assertTrue(
                officeRepository.existsById(hierarchy.officeId())
        );
        assertTrue(
                roomRepository.existsById(hierarchy.roomId())
        );
        assertFalse(
                resourceRepository.existsById(hierarchy.resourceId())
        );
        assertFalse(
                reservationRepository.existsById(
                        hierarchy.reservationId()
                )
        );
        assertTrue(
                appUserRepository.existsById(hierarchy.appUserId())
        );
    }

    @Test
    void deleteMethods_shouldReturnFalseWhenEntityDoesNotExist() {
        long unknownId = Long.MAX_VALUE;

        assertFalse(officeService.deleteOffice(unknownId));
        assertFalse(roomService.deleteRoom(unknownId));
        assertFalse(resourceService.deleteResource(unknownId));
    }

    private TestHierarchy createHierarchy() {
        LocalDateTime createdAt =
                LocalDateTime.of(2026, 1, 1, 8, 0);

        AppUser appUser = appUserRepository.save(
                new AppUser(
                        "test-entra-oid",
                        "cascade-test@example.com",
                        "Cascade Test User",
                        AppRole.USER,
                        createdAt
                )
        );

        Office office = officeRepository.save(
                new Office(
                        "Test Office",
                        "Office für den Kaskadentest",
                        true,
                        1200,
                        700
                )
        );

        Room room = roomRepository.save(
                new Room(
                        office,
                        "Test Room",
                        "Raum für den Kaskadentest",
                        true,
                        20,
                        20,
                        800,
                        500
                )
        );

        Resource resource = resourceRepository.save(
                new Resource(
                        "Test Desk",
                        "Arbeitsplatz für den Kaskadentest",
                        ResourceType.DESK,
                        room,
                        true,
                        true,
                        50,
                        50,
                        108,
                        74
                )
        );

        Reservation reservation = reservationRepository.save(
                new Reservation(
                        appUser,
                        resource,
                        createdAt.plusDays(1),
                        createdAt.plusDays(1).plusHours(8),
                        "Test Reservation",
                        "Reservierung für den Kaskadentest",
                        ReservationStatus.ACTIVE,
                        createdAt
                )
        );

        entityManager.flush();
        entityManager.clear();

        return new TestHierarchy(
                appUser.getId(),
                office.getId(),
                room.getId(),
                resource.getId(),
                reservation.getId()
        );
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record TestHierarchy(
            Long appUserId,
            Long officeId,
            Long roomId,
            Long resourceId,
            Long reservationId
    ) {
    }
}