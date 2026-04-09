CREATE SEQUENCE booking_id_seq START 1 INCREMENT 1;

CREATE TABLE bookings (
    id              VARCHAR(20)   NOT NULL,
    event_id        VARCHAR(20)   NOT NULL,
    attendee_name   VARCHAR(255)  NOT NULL,
    attendee_email  VARCHAR(255)  NOT NULL,
    booked_at       TIMESTAMP     NOT NULL DEFAULT now(),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMP     NULL,

    CONSTRAINT pk_bookings        PRIMARY KEY (id),
    CONSTRAINT fk_bookings_event  FOREIGN KEY (event_id) REFERENCES events (id)
);

CREATE INDEX idx_bookings_event_id ON bookings (event_id)       WHERE deleted_at IS NULL;
CREATE INDEX idx_bookings_email    ON bookings (attendee_email)  WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uq_active_booking_email_event
    ON bookings (event_id, attendee_email)
    WHERE deleted_at IS NULL;
