package com.digicore.eventapi.services;

import com.digicore.eventapi.archive.ArchiveService;
import com.digicore.eventapi.dto.request.CreateEventRequest;
import com.digicore.eventapi.dto.response.EventResponse;
import com.digicore.eventapi.exception.ResourceNotFoundException;
import com.digicore.eventapi.models.Event;
import com.digicore.eventapi.models.enums.EventStatus;
import com.digicore.eventapi.repositories.EventRepository;
import com.digicore.eventapi.utils.EventCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository   eventRepository;
    @Mock ArchiveService    archiveService;
    @Mock EventCacheService eventCacheService;

    @InjectMocks EventService eventService;

    private CreateEventRequest validRequest;
    private Event              sampleEvent;

    @BeforeEach
    void setUp() {
        validRequest = new CreateEventRequest();
        validRequest.setTitle("Test Event");
        validRequest.setDescription("Description");
        validRequest.setEventDate(LocalDateTime.now().plusDays(10));
        validRequest.setVenue("Test Venue");
        validRequest.setTotalSeats(50);

        sampleEvent = Event.builder()
                .id("EVT-0001")
                .title("Test Event")
                .description("Description")
                .eventDate(validRequest.getEventDate())
                .venue("Test Venue")
                .totalSeats(50)
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("createEvent — saves and returns EventResponse")
    void createEvent_success() {
        when(eventRepository.save(any(Event.class))).thenReturn(sampleEvent);

        EventResponse response = eventService.createEvent(validRequest);

        assertThat(response.getId()).isEqualTo("EVT-0001");
        assertThat(response.getTotalSeats()).isEqualTo(50);
        assertThat(response.getStatus()).isEqualTo(EventStatus.OPEN);
        verify(eventCacheService).put(any(EventResponse.class));
    }

    @Test
    @DisplayName("getEvent — returns from cache when present")
    void getEvent_cacheHit() {
        EventResponse cached = EventResponse.from(sampleEvent);
        when(eventCacheService.get("EVT-0001")).thenReturn(Optional.of(cached));

        EventResponse response = eventService.getEvent("EVT-0001");

        assertThat(response).isEqualTo(cached);
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("getEvent — fetches from DB on cache miss")
    void getEvent_cacheMiss() {
        when(eventCacheService.get("EVT-0001")).thenReturn(Optional.empty());
        when(eventRepository.findActiveById("EVT-0001")).thenReturn(Optional.of(sampleEvent));

        EventResponse response = eventService.getEvent("EVT-0001");

        assertThat(response.getTitle()).isEqualTo("Test Event");
        verify(eventCacheService).put(any(EventResponse.class));
    }

    @Test
    @DisplayName("getEvent — throws 404 for unknown ID")
    void getEvent_notFound() {
        when(eventCacheService.get("EVT-9999")).thenReturn(Optional.empty());
        when(eventRepository.findActiveById("EVT-9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent("EVT-9999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listEvents — returns page")
    void listEvents_returnsPage() {
        when(eventRepository.findAllActive(any())).thenReturn(new PageImpl<>(List.of(sampleEvent)));

        Page<EventResponse> result = eventService.listEvents(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("EVT-0001");
    }

    @Test
    @DisplayName("autoCloseIfFull — sets status CLOSED when seats exhausted")
    void autoCloseIfFull_closesEvent() {
        sampleEvent.setBookedSeats(50);
        when(eventRepository.save(sampleEvent)).thenReturn(sampleEvent);

        eventService.autoCloseIfFull(sampleEvent);

        assertThat(sampleEvent.getStatus()).isEqualTo(EventStatus.CLOSED);
        verify(eventCacheService).evict("EVT-0001");
    }
}
