package com.digicore.eventapi.controllers;

import com.digicore.eventapi.dto.request.CreateEventRequest;
import com.digicore.eventapi.dto.response.ApiResponse;
import com.digicore.eventapi.dto.response.EventResponse;
import com.digicore.eventapi.services.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event management endpoints")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody CreateEventRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Event created successfully",
                        eventService.createEvent(request)));
    }

    @GetMapping
    @Operation(summary = "List all events (paginated)")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @Parameter(description = "asc or desc") @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(ApiResponse.success("Events retrieved successfully",
                eventService.listEvents(PageRequest.of(page, size, sort))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(
            @Parameter(description = "Event ID, e.g. EVT-0001") @PathVariable String id) {

        return ResponseEntity.ok(ApiResponse.success("Event retrieved successfully",
                eventService.getEvent(id)));
    }
}
