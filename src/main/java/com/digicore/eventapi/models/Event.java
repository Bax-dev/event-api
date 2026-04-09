package com.digicore.eventapi.models;

import com.digicore.eventapi.archive.SoftDeletable;
import com.digicore.eventapi.models.enums.EventStatus;
import com.digicore.eventapi.utils.EventIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event implements SoftDeletable {

    @Id
    @GeneratedValue(generator = "event_id_gen")
    @GenericGenerator(name = "event_id_gen", type = EventIdGenerator.class)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private String venue;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "booked_seats", nullable = false)
    private int bookedSeats = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.OPEN;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public int remainingSeats() {
        return totalSeats - bookedSeats;
    }
}
