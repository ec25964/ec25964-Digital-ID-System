package com.github.ec25964.model;

import java.time.LocalDateTime;

public class AuditEntry {

    private final AuditEventType eventType;
    private final String digitalIdId;
    private final String organisation;
    private final LocalDateTime timestamp;
    private final String details;

    public AuditEntry(AuditEventType eventType, String digitalIdId, String organisation,
            LocalDateTime timestamp, String details) {
        this.eventType = eventType;
        this.digitalIdId = digitalIdId;
        this.organisation = organisation;
        this.timestamp = timestamp;
        this.details = details;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getDigitalIdId() {
        return digitalIdId;
    }

    public String getOrganisation() {
        return organisation;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
