package io.github.hummelhose.desksharing.application.service;

import io.github.hummelhose.desksharing.domain.model.AppUser;
import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import io.github.hummelhose.desksharing.domain.model.Resource;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.AppUserRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ReservationRepository;
import io.github.hummelhose.desksharing.infrastructure.persistence.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationService {

    private static final Duration MAX_RESERVATION_DURATION = Duration.ofDays(1);
    private static final int MAX_DAYS_IN_FUTURE = 30;

    private final ReservationRepository reservationRepository;
    private final AppUserRepository appUserRepository;
    private final ResourceRepository resourceRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              AppUserRepository appUserRepository,
                              ResourceRepository resourceRepository) {
        this.reservationRepository = reservationRepository;
        this.appUserRepository = appUserRepository;
        this.resourceRepository = resourceRepository;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsByAppUserId(Long appUserId) {
        return reservationRepository.findByAppUserId(appUserId);
    }

    public List<Reservation> getReservationsByResourceId(Long resourceId) {
        return reservationRepository.findByResourceId(resourceId);
    }

    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    public Optional<Reservation> getCurrentReservationForResource(Long resourceId, LocalDateTime now) {
        return reservationRepository
                .findFirstByResourceIdAndStatusAndStartDateTimeLessThanEqualAndEndDateTimeAfterOrderByEndDateTimeAsc(
                        resourceId,
                        ReservationStatus.ACTIVE,
                        now,
                        now
                );
    }

    public Optional<Reservation> getNextReservationTodayForResource(Long resourceId, LocalDateTime now) {
        LocalDateTime tomorrowStart = now.toLocalDate()
                .plusDays(1)
                .atStartOfDay();

        return reservationRepository
                .findFirstByResourceIdAndStatusAndStartDateTimeAfterAndStartDateTimeBeforeOrderByStartDateTimeAsc(
                        resourceId,
                        ReservationStatus.ACTIVE,
                        now,
                        tomorrowStart
                );
    }

    public Optional<Reservation> createReservation(Long appUserId,
                                                   Long resourceId,
                                                   LocalDateTime startDateTime,
                                                   LocalDateTime endDateTime,
                                                   String title,
                                                   String notes) {
        ReservationCreationResult result = createReservationWithResult(
                appUserId,
                resourceId,
                startDateTime,
                endDateTime,
                title,
                notes
        );

        return result.reservation();
    }

    public ReservationCreationResult createReservationWithResult(Long appUserId,
                                                                 Long resourceId,
                                                                 LocalDateTime startDateTime,
                                                                 LocalDateTime endDateTime,
                                                                 String title,
                                                                 String notes) {
        LocalDateTime now = LocalDateTime.now();

        if (startDateTime == null || endDateTime == null) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.INVALID_TIME_RANGE,
                    "Bitte Start und Ende vollständig ausfüllen."
            );
        }

        if (!endDateTime.isAfter(startDateTime)) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.INVALID_TIME_RANGE,
                    "Die Endzeit muss nach der Startzeit liegen."
            );
        }

        if (startDateTime.isBefore(now.minusMinutes(1))) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.START_IN_PAST,
                    "Die Startzeit darf nicht in der Vergangenheit liegen."
            );
        }

        if (Duration.between(startDateTime, endDateTime).compareTo(MAX_RESERVATION_DURATION) > 0) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.TOO_LONG,
                    "Eine Buchung darf maximal 1 Tag dauern."
            );
        }

        if (startDateTime.isAfter(now.plusDays(MAX_DAYS_IN_FUTURE))
                || endDateTime.isAfter(now.plusDays(MAX_DAYS_IN_FUTURE))) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.TOO_FAR_IN_FUTURE,
                    "Eine Buchung darf maximal 30 Tage im Voraus erstellt werden."
            );
        }

        Optional<AppUser> appUserOptional = appUserRepository.findById(appUserId);
        if (appUserOptional.isEmpty()) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.USER_NOT_FOUND,
                    "Der angemeldete Benutzer wurde nicht gefunden."
            );
        }

        Optional<Resource> resourceOptional = resourceRepository.findById(resourceId);
        if (resourceOptional.isEmpty()) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.RESOURCE_NOT_FOUND,
                    "Der ausgewählte Tisch wurde nicht gefunden."
            );
        }

        Resource resource = resourceOptional.get();

        if (!resource.isActive()) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.RESOURCE_INACTIVE,
                    "Dieser Tisch ist aktuell inaktiv und kann nicht gebucht werden."
            );
        }

        if (!resource.isBookable()) {
            return ReservationCreationResult.failed(
                    ReservationCreationStatus.RESOURCE_NOT_BOOKABLE,
                    "Dieser Tisch ist aktuell gesperrt und kann nicht gebucht werden."
            );
        }

        List<Reservation> overlappingReservationsForResource = reservationRepository.findOverlappingReservations(
                resourceId,
                ReservationStatus.ACTIVE,
                startDateTime,
                endDateTime
        );

        if (!overlappingReservationsForResource.isEmpty()) {
            Reservation existingReservation = overlappingReservationsForResource.get(0);

            return ReservationCreationResult.failed(
                    ReservationCreationStatus.RESOURCE_ALREADY_BOOKED,
                    "Dieser Tisch ist in diesem Zeitraum bereits von "
                            + existingReservation.getAppUser().getDisplayName()
                            + " gebucht."
            );
        }

        List<Reservation> overlappingReservationsForUser = reservationRepository.findOverlappingReservationsForUser(
                appUserId,
                ReservationStatus.ACTIVE,
                startDateTime,
                endDateTime
        );

        if (!overlappingReservationsForUser.isEmpty()) {
            Reservation existingReservation = overlappingReservationsForUser.get(0);

            return ReservationCreationResult.failed(
                    ReservationCreationStatus.USER_ALREADY_HAS_BOOKING,
                    "Du hast in diesem Zeitraum bereits eine Buchung für "
                            + existingReservation.getResource().getName()
                            + "."
            );
        }

        Reservation reservation = new Reservation(
                appUserOptional.get(),
                resource,
                startDateTime,
                endDateTime,
                normalizeTitle(title),
                normalizeNotes(notes),
                ReservationStatus.ACTIVE,
                LocalDateTime.now()
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        return ReservationCreationResult.success(savedReservation);
    }

    public Optional<Reservation> cancelReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .map(existingReservation -> {
                    existingReservation.setStatus(ReservationStatus.CANCELLED);
                    return reservationRepository.save(existingReservation);
                });
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Sitzplatzbuchung";
        }

        return title.trim();
    }

    private String normalizeNotes(String notes) {
        if (notes == null) {
            return "";
        }

        return notes.trim();
    }

    public enum ReservationCreationStatus {
        SUCCESS,
        INVALID_TIME_RANGE,
        START_IN_PAST,
        TOO_LONG,
        TOO_FAR_IN_FUTURE,
        USER_NOT_FOUND,
        RESOURCE_NOT_FOUND,
        RESOURCE_INACTIVE,
        RESOURCE_NOT_BOOKABLE,
        RESOURCE_ALREADY_BOOKED,
        USER_ALREADY_HAS_BOOKING
    }

    public record ReservationCreationResult(
            boolean success,
            ReservationCreationStatus status,
            String message,
            Optional<Reservation> reservation
    ) {

        public static ReservationCreationResult success(Reservation reservation) {
            return new ReservationCreationResult(
                    true,
                    ReservationCreationStatus.SUCCESS,
                    "Sitzplatz wurde gebucht.",
                    Optional.of(reservation)
            );
        }

        public static ReservationCreationResult failed(ReservationCreationStatus status, String message) {
            return new ReservationCreationResult(
                    false,
                    status,
                    message,
                    Optional.empty()
            );
        }
    }
}