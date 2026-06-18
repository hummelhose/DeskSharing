package io.github.hummelhose.desksharing.infrastructure.persistence.repository;

import io.github.hummelhose.desksharing.domain.model.Reservation;
import io.github.hummelhose.desksharing.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByAppUserId(Long appUserId);

    List<Reservation> findByResourceId(Long resourceId);

    List<Reservation> findByStatus(ReservationStatus status);

    Optional<Reservation> findFirstByResourceIdAndStatusAndStartDateTimeLessThanEqualAndEndDateTimeAfterOrderByEndDateTimeAsc(
            Long resourceId,
            ReservationStatus status,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime
    );

    Optional<Reservation> findFirstByResourceIdAndStatusAndStartDateTimeAfterAndStartDateTimeBeforeOrderByStartDateTimeAsc(
            Long resourceId,
            ReservationStatus status,
            LocalDateTime after,
            LocalDateTime before
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.resource.id = :resourceId
              AND r.status = :status
              AND r.startDateTime < :endDateTime
              AND r.endDateTime > :startDateTime
            """)
    List<Reservation> findOverlappingReservations(
            @Param("resourceId") Long resourceId,
            @Param("status") ReservationStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("""
            SELECT r
            FROM Reservation r
            WHERE r.appUser.id = :appUserId
              AND r.status = :status
              AND r.startDateTime < :endDateTime
              AND r.endDateTime > :startDateTime
            """)
    List<Reservation> findOverlappingReservationsForUser(
            @Param("appUserId") Long appUserId,
            @Param("status") ReservationStatus status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );
}