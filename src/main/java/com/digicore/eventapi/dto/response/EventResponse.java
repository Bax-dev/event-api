package com.digicore.eventapi.dto.response;

import com.digicore.eventapi.models.Event;
import com.digicore.eventapi.models.enums.EventStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event details response")
public class EventResponse {

    @Schema(example = "EVT-0001")
    private String id;

    @Schema(example = "Spring Boot Workshop")
    private String title;

    private String description;

    @Schema(example = "2026-06-15T10:00:00")
    private LocalDateTime eventDate;

    @Schema(example = "Lagos Tech Hub, Victoria Island")
    private String venue;

    @Schema(example = "100")
    private int totalSeats;

    @Schema(example = "45")
    private int bookedSeats;

    @Schema(example = "55")
    private int remainingSeats;

    @Schema(example = "OPEN")
    private EventStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .venue(event.getVenue())
                .totalSeats(event.getTotalSeats())
                .bookedSeats(event.getBookedSeats())
                .remainingSeats(event.remainingSeats())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
