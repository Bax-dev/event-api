package com.digicore.eventapi.archive;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Central service for performing soft deletes.
 * Stamps deletedAt and lets JPA persist the change — no physical DELETE issued.
 */
@Service
public class ArchiveService {

    /**
     * Soft-delete any entity that implements {@link SoftDeletable}.
     *
     * @param entity the entity to archive
     */
    public <T extends SoftDeletable> void archive(T entity) {
        if (!entity.isDeleted()) {
            entity.setDeletedAt(LocalDateTime.now());
        }
    }

    /**
     * Restore a soft-deleted entity (clear the deletedAt timestamp).
     *
     * @param entity the entity to restore
     */
    public <T extends SoftDeletable> void restore(T entity) {
        entity.setDeletedAt(null);
    }
}
