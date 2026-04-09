package com.digicore.eventapi.services;

import com.digicore.eventapi.archive.ArchiveService;
import com.digicore.eventapi.dto.request.CreateBookingRequest;
import com.digicore.eventapi.dto.response.BookingResponse;
import com.digicore.eventapi.exception.BusinessException;
import com.digicore.eventapi.exception.ResourceNotFoundException;
import com.digicore.eventapi.models.Booking;
import com.digicore.eventapi.models.Event;
import com.digicore.eventapi.models.enums.EventStatus;
import com.digicore.eventapi.repositories.BookingRepository;
import com.digicore.eventapi.utils.EventCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventService      eventService;
    private final ArchiveService    archiveService;
    private final EventCacheService eventCacheService;

    @Transactional
    public BookingResponse createBooking(String eventId, CreateBookingRequest request) {
        // Pessimistic write lock prevents concurrent overbooking
        Event event = eventService.requireActiveEventForUpdate(eventId);

        if (event.getStatus() == EventStatus.CLOSED) {
            throw new BusinessException("Event is CLOSED and no longer accepting bookings.");
        }

        if (event.getBookedSeats() >= event.getTotalSeats()) {
            throw new BusinessException("Event is fully booked. No seats available.");
        }

        String email = request.getAttendeeEmail().toLowerCase().trim();
        if (bookingRepository.existsActiveBooking(eventId, email)) {
            throw new BusinessException("'" + email + "' has already booked this event.");
        }

        Booking booking = Booking.builder()
                .event(event)
                .attendeeName(request.getAttendeeName().trim())
                .attendeeEmail(email)
                .build();

        Booking saved = bookingRepository.save(booking);

        event.setBookedSeats(event.getBookedSeats() + 1);
        eventService.autoCloseIfFull(event);
        eventCacheService.evict(eventId);

        log.info("Booking created: id={}, event={}", saved.getId(), eventId);
        return BookingResponse.from(saved);
    }

    @Transactional
    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findActiveById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        Event event = eventService.requireActiveEventForUpdate(booking.getEvent().getId());

        archiveService.archive(booking);
        bookingRepository.save(booking);

        int newCount = Math.max(0, event.getBookedSeats() - 1);
        event.setBookedSeats(newCount);

        if (event.getStatus() == EventStatus.CLOSED && newCount < event.getTotalSeats()) {
            event.setStatus(EventStatus.OPEN);
            log.info("Event {} re-opened after cancellation", event.getId());
        }

        eventCacheService.evict(event.getId());
        log.info("Booking cancelled: id={}", bookingId);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> listBookingsForEvent(String eventId, Pageable pageable) {
        eventService.requireActiveEvent(eventId);
        return bookingRepository.findActiveByEventId(eventId, pageable).map(BookingResponse::from);
    }
}
