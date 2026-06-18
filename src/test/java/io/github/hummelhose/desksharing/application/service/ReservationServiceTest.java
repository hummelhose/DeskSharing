package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ReservationRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.INVALID_TIME_RANGE;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.RESOURCE_ALREADY_BOOKED;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.RESOURCE_NOT_BOOKABLE;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.START_IN_PAST;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.SUCCESS;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.TOO_FAR_IN_FUTURE;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.TOO_LONG;
import static io.github.hummelhose.desksharing.application.service.ReservationService.ReservationCreationStatus.USER_ALREADY_HAS_BOOKING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ResourceRepository resourceRepository;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                appUserRepository,
                resourceRepository
        );
    }

    @Test
    void createReservationWithResult_shouldCreateReservation_whenDataIsValid() {
        Long appUserId = 1L;
        Long resourceId = 10L;

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        AppUser appUser = org.mockito.Mockito.mock(AppUser.class);
        Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(appUserRepository.findById(appUserId)).thenReturn(Optional.of(appUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resource.isActive()).thenReturn(true);
        when(resource.isBookable()).thenReturn(true);

        when(reservationRepository.findOverlappingReservations(
                resourceId,
                ReservationStatus.ACTIVE,
                start,
                end
        )).thenReturn(List.of());

        when(reservationRepository.findOverlappingReservationsForUser(
                appUserId,
                ReservationStatus.ACTIVE,
                start,
                end
        )).thenReturn(List.of());

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        appUserId,
                        resourceId,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        "Test"
                );

        assertTrue(result.success());
        assertEquals(SUCCESS, result.status());
        assertTrue(result.reservation().isPresent());

        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservationWithResult_shouldFail_whenEndIsBeforeStart() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = start.minusHours(1);

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        1L,
                        10L,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(INVALID_TIME_RANGE, result.status());
        assertTrue(result.reservation().isEmpty());

        verifyNoInteractions(appUserRepository);
        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservationWithResult_shouldFail_whenStartIsInPast() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        1L,
                        10L,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(START_IN_PAST, result.status());

        verifyNoInteractions(appUserRepository);
        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservationWithResult_shouldFail_whenReservationIsLongerThanOneDay() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusDays(1).plusMinutes(1);

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        1L,
                        10L,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(TOO_LONG, result.status());

        verifyNoInteractions(appUserRepository);
        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservationWithResult_shouldFail_whenReservationIsMoreThanThirtyDaysInFuture() {
        LocalDateTime start = LocalDateTime.now().plusDays(31);
        LocalDateTime end = start.plusHours(2);

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        1L,
                        10L,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(TOO_FAR_IN_FUTURE, result.status());

        verifyNoInteractions(appUserRepository);
        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void createReservationWithResult_shouldFail_whenResourceIsNotBookable() {
        Long appUserId = 1L;
        Long resourceId = 10L;

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        AppUser appUser = org.mockito.Mockito.mock(AppUser.class);
        Resource resource = org.mockito.Mockito.mock(Resource.class);

        when(appUserRepository.findById(appUserId)).thenReturn(Optional.of(appUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resource.isActive()).thenReturn(true);
        when(resource.isBookable()).thenReturn(false);

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        appUserId,
                        resourceId,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(RESOURCE_NOT_BOOKABLE, result.status());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void createReservationWithResult_shouldFail_whenResourceAlreadyHasBookingInSameTimeRange() {
        Long appUserId = 1L;
        Long resourceId = 10L;

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        AppUser appUser = org.mockito.Mockito.mock(AppUser.class);
        Resource resource = org.mockito.Mockito.mock(Resource.class);

        AppUser existingUser = org.mockito.Mockito.mock(AppUser.class);
        Reservation existingReservation = org.mockito.Mockito.mock(Reservation.class);

        when(existingUser.getDisplayName()).thenReturn("Max Mustermann");
        when(existingReservation.getAppUser()).thenReturn(existingUser);

        when(appUserRepository.findById(appUserId)).thenReturn(Optional.of(appUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resource.isActive()).thenReturn(true);
        when(resource.isBookable()).thenReturn(true);

        when(reservationRepository.findOverlappingReservations(
                resourceId,
                ReservationStatus.ACTIVE,
                start,
                end
        )).thenReturn(List.of(existingReservation));

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        appUserId,
                        resourceId,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(RESOURCE_ALREADY_BOOKED, result.status());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void createReservationWithResult_shouldFail_whenUserAlreadyHasBookingInSameTimeRange() {
        Long appUserId = 1L;
        Long resourceId = 10L;

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        AppUser appUser = org.mockito.Mockito.mock(AppUser.class);
        Resource resource = org.mockito.Mockito.mock(Resource.class);

        Resource existingResource = org.mockito.Mockito.mock(Resource.class);
        Reservation existingReservation = org.mockito.Mockito.mock(Reservation.class);

        when(existingResource.getName()).thenReturn("Tisch 2");
        when(existingReservation.getResource()).thenReturn(existingResource);

        when(appUserRepository.findById(appUserId)).thenReturn(Optional.of(appUser));
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(resource.isActive()).thenReturn(true);
        when(resource.isBookable()).thenReturn(true);

        when(reservationRepository.findOverlappingReservations(
                resourceId,
                ReservationStatus.ACTIVE,
                start,
                end
        )).thenReturn(List.of());

        when(reservationRepository.findOverlappingReservationsForUser(
                appUserId,
                ReservationStatus.ACTIVE,
                start,
                end
        )).thenReturn(List.of(existingReservation));

        ReservationService.ReservationCreationResult result =
                reservationService.createReservationWithResult(
                        appUserId,
                        resourceId,
                        start,
                        end,
                        "Sitzplatzbuchung",
                        ""
                );

        assertFalse(result.success());
        assertEquals(USER_ALREADY_HAS_BOOKING, result.status());

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void cancelReservation_shouldSetStatusToCancelled() {
        Long reservationId = 100L;

        Reservation reservation = org.mockito.Mockito.mock(Reservation.class);

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Optional<Reservation> result = reservationService.cancelReservation(reservationId);

        assertTrue(result.isPresent());

        verify(reservation).setStatus(ReservationStatus.CANCELLED);
        verify(reservationRepository).save(reservation);
    }
}