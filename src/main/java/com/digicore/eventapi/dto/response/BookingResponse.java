package com.digicore.eventapi.dto.response;

import com.digicore.eventapi.models.Booking;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking details response")
public class BookingResponse {

    @Schema(example = "BK-0001")
    private String id;

    @Schema(example = "EVT-0001")
    private String eventId;

    @Schema(example = "Spring Boot Workshop")
    private String eventTitle;

    @Schema(example = "John Doe")
    private String attendeeName;

    @Schema(example = "john.doe@example.com")
    private String attendeeEmail;

    private LocalDateTime bookedAt;

    public static BookingResponse from(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .attendeeName(booking.getAttendeeName())
                .attendeeEmail(booking.getAttendeeEmail())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
