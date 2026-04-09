package com.digicore.eventapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payload for creating a new event")
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Event title", example = "Spring Boot Workshop")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Event description", example = "A hands-on workshop covering Spring Boot 3.x")
    private String description;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    @Schema(description = "Event date and time (must be future)", example = "2026-06-15T10:00:00")
    private LocalDateTime eventDate;

    @NotBlank(message = "Venue is required")
    @Size(max = 500, message = "Venue must not exceed 500 characters")
    @Schema(description = "Venue / location", example = "Lagos Tech Hub, Victoria Island")
    private String venue;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Schema(description = "Maximum seat capacity", example = "100")
    private Integer totalSeats;
}
