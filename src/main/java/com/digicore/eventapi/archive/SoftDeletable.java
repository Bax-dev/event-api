package com.digicore.eventapi.archive;

import java.time.LocalDateTime;

/**
 * Contract for entities that support soft deletion.
 * Implementing classes store a deletedAt timestamp instead of being physically removed.
 */
public interface SoftDeletable {

    LocalDateTime getDeletedAt();

    void setDeletedAt(LocalDateTime deletedAt);

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    default void markDeleted() {
        setDeletedAt(LocalDateTime.now());
    }
}
