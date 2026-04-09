CREATE SEQUENCE event_id_seq START 1 INCREMENT 1;

CREATE TABLE events (
    id           VARCHAR(20)   NOT NULL,
    title        VARCHAR(255)  NOT NULL,
    description  TEXT,
    event_date   TIMESTAMP     NOT NULL,
    venue        VARCHAR(500)  NOT NULL,
    total_seats  INT           NOT NULL,
    booked_seats INT           NOT NULL DEFAULT 0,
    status       VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMP     NULL,

    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT chk_events_total_seats  CHECK (total_seats > 0),
    CONSTRAINT chk_events_booked_seats CHECK (booked_seats >= 0),
    CONSTRAINT chk_events_status       CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX idx_events_status     ON events (status)     WHERE deleted_at IS NULL;
CREATE INDEX idx_events_event_date ON events (event_date) WHERE deleted_at IS NULL;
