package com.digicore.eventapi.services;

import com.digicore.eventapi.archive.ArchiveService;
import com.digicore.eventapi.dto.request.CreateEventRequest;
import com.digicore.eventapi.dto.response.EventResponse;
import com.digicore.eventapi.exception.ResourceNotFoundException;
import com.digicore.eventapi.models.Event;
import com.digicore.eventapi.models.enums.EventStatus;
import com.digicore.eventapi.repositories.EventRepository;
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
public class EventService {

    private final EventRepository   eventRepository;
    private final ArchiveService    archiveService;
    private final EventCacheService eventCacheService;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .venue(request.getVenue())
                .totalSeats(request.getTotalSeats())
                .bookedSeats(0)
                .status(EventStatus.OPEN)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Event created: id={}", saved.getId());

        EventResponse response = EventResponse.from(saved);
        eventCacheService.put(response);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listEvents(Pageable pageable) {
        return eventRepository.findAllActive(pageable).map(EventResponse::from);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(String id) {
        return eventCacheService.get(id).orElseGet(() -> {
            Event event = requireActiveEvent(id);
            EventResponse response = EventResponse.from(event);
            eventCacheService.put(response);
            return response;
        });
    }

    @Transactional
    public void autoCloseIfFull(Event event) {
        if (event.getBookedSeats() >= event.getTotalSeats()
                && event.getStatus() == EventStatus.OPEN) {
            event.setStatus(EventStatus.CLOSED);
            eventRepository.save(event);
            eventCacheService.evict(event.getId());
            log.info("Event {} auto-closed (fully booked)", event.getId());
        }
    }

    public Event requireActiveEvent(String id) {
        return eventRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    public Event requireActiveEventForUpdate(String id) {
        return eventRepository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }
}
