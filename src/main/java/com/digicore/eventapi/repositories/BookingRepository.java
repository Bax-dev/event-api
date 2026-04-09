package com.digicore.eventapi.repositories;

import com.digicore.eventapi.models.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {

    @Query("SELECT b FROM Booking b WHERE b.event.id = :eventId AND b.deletedAt IS NULL")
    Page<Booking> findActiveByEventId(@Param("eventId") String eventId, Pageable pageable);

    @Query("SELECT COUNT(b) > 0 FROM Booking b " +
           "WHERE b.event.id = :eventId AND b.attendeeEmail = :email AND b.deletedAt IS NULL")
    boolean existsActiveBooking(@Param("eventId") String eventId, @Param("email") String email);

    @Query("SELECT b FROM Booking b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Booking> findActiveById(@Param("id") String id);
}
