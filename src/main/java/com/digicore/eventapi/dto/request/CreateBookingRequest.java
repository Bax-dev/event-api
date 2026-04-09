package com.digicore.eventapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload for booking a seat at an event")
public class CreateBookingRequest {

    @NotBlank(message = "Attendee name is required")
    @Size(max = 255, message = "Attendee name must not exceed 255 characters")
    @Schema(description = "Full name of the attendee", example = "John Doe")
    private String attendeeName;

    @NotBlank(message = "Attendee email is required")
    @Email(message = "Attendee email must be a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Email address of the attendee", example = "john.doe@example.com")
    private String attendeeEmail;
}
