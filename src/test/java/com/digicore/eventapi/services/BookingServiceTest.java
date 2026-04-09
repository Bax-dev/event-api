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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock EventService      eventService;
    @Mock ArchiveService    archiveService;
    @Mock EventCacheService eventCacheService;

    @InjectMocks BookingService bookingService;

    private Event                openEvent;
    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        openEvent = Event.builder()
                .id("EVT-0001")
                .title("Open Event")
                .totalSeats(5)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build();

        request = new CreateBookingRequest();
        request.setAttendeeName("Jane Doe");
        request.setAttendeeEmail("jane@example.com");
    }

    @Test
    @DisplayName("createBooking — success increments bookedSeats")
    void createBooking_success() {
        Booking saved = Booking.builder()
                .id("BK-0001")
                .event(openEvent)
                .attendeeName("Jane Doe")
                .attendeeEmail("jane@example.com")
                .bookedAt(LocalDateTime.now())
                .build();

        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);
        when(bookingRepository.existsActiveBooking("EVT-0001", "jane@example.com")).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(saved);

        BookingResponse response = bookingService.createBooking("EVT-0001", request);

        assertThat(response.getId()).isEqualTo("BK-0001");
        assertThat(openEvent.getBookedSeats()).isEqualTo(1);
        verify(eventService).autoCloseIfFull(openEvent);
        verify(eventCacheService).evict("EVT-0001");
    }

    @Test
    @DisplayName("createBooking — rejects CLOSED event")
    void createBooking_closedEvent() {
        openEvent.setStatus(EventStatus.CLOSED);
        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);

        assertThatThrownBy(() -> bookingService.createBooking("EVT-0001", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    @DisplayName("createBooking — rejects when fully booked")
    void createBooking_noCapacity() {
        openEvent.setBookedSeats(5);
        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);

        assertThatThrownBy(() -> bookingService.createBooking("EVT-0001", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fully booked");
    }

    @Test
    @DisplayName("createBooking — rejects duplicate email")
    void createBooking_duplicateEmail() {
        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);
        when(bookingRepository.existsActiveBooking("EVT-0001", "jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking("EVT-0001", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    @DisplayName("cancelBooking — soft deletes and decrements bookedSeats")
    void cancelBooking_success() {
        openEvent.setBookedSeats(3);
        Booking booking = Booking.builder()
                .id("BK-0001")
                .event(openEvent)
                .attendeeName("Jane Doe")
                .attendeeEmail("jane@example.com")
                .build();

        when(bookingRepository.findActiveById("BK-0001")).thenReturn(Optional.of(booking));
        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);

        bookingService.cancelBooking("BK-0001");

        verify(archiveService).archive(booking);
        verify(bookingRepository).save(booking);
        assertThat(openEvent.getBookedSeats()).isEqualTo(2);
        verify(eventCacheService).evict("EVT-0001");
    }

    @Test
    @DisplayName("cancelBooking — throws 404 for unknown booking")
    void cancelBooking_notFound() {
        when(bookingRepository.findActiveById("BK-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancelBooking("BK-9999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cancelBooking — re-opens CLOSED event when seat freed")
    void cancelBooking_reopensEvent() {
        openEvent.setBookedSeats(5);
        openEvent.setStatus(EventStatus.CLOSED);

        Booking booking = Booking.builder()
                .id("BK-0001")
                .event(openEvent)
                .attendeeName("Jane")
                .attendeeEmail("jane@example.com")
                .build();

        when(bookingRepository.findActiveById("BK-0001")).thenReturn(Optional.of(booking));
        when(eventService.requireActiveEventForUpdate("EVT-0001")).thenReturn(openEvent);

        bookingService.cancelBooking("BK-0001");

        assertThat(openEvent.getStatus()).isEqualTo(EventStatus.OPEN);
        assertThat(openEvent.getBookedSeats()).isEqualTo(4);
    }
}
