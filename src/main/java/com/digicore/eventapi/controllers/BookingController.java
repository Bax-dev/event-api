package com.digicore.eventapi.controllers;

import com.digicore.eventapi.dto.request.CreateBookingRequest;
import com.digicore.eventapi.dto.response.ApiResponse;
import com.digicore.eventapi.dto.response.BookingResponse;
import com.digicore.eventapi.services.BookingService;
import com.digicore.eventapi.utils.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingService     bookingService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper       objectMapper;

    @PostMapping("/events/{id}/bookings")
    @Operation(
        summary = "Book a seat at an event",
        description = "Supply **X-Idempotency-Key** header to make this request idempotent. " +
                      "Duplicate requests with the same key return the original response for 24 h."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Parameter(description = "Event ID, e.g. EVT-0001") @PathVariable String id,
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {

        // Return cached response for duplicate requests
        if (idempotencyKey != null) {
            var cached = idempotencyService.getCachedResponse(idempotencyKey);
            if (cached.isPresent()) {
                try {
                    @SuppressWarnings("unchecked")
                    ApiResponse<BookingResponse> body =
                            objectMapper.readValue(cached.get(), ApiResponse.class);
                    return ResponseEntity.ok(body);
                } catch (JsonProcessingException e) {
                    log.warn("Could not deserialize cached idempotency response", e);
                }
            }
        }

        BookingResponse booking = bookingService.createBooking(id, request);
        ApiResponse<BookingResponse> response = ApiResponse.success("Booking created successfully", booking);

        if (idempotencyKey != null) {
            try {
                idempotencyService.storeResponse(idempotencyKey, objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException e) {
                log.warn("Could not store idempotency response", e);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/bookings/{id}")
    @Operation(summary = "Cancel a booking", description = "Soft-deletes the booking and frees the seat.")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @Parameter(description = "Booking ID, e.g. BK-0001") @PathVariable String id) {

        bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", null));
    }

    @GetMapping("/events/{id}/bookings")
    @Operation(summary = "List all bookings for an event (paginated)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> listBookings(
            @Parameter(description = "Event ID, e.g. EVT-0001") @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully",
                bookingService.listBookingsForEvent(id,
                        PageRequest.of(page, size, Sort.by("bookedAt").descending()))));
    }
}
