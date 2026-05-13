package com.github.ec25964.service;

import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.persistence.AuditRepository;

import java.time.LocalDate;
import java.util.List;

public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void log(AuditEntry entry) {
        auditRepository.append(entry);
    }

    public List<AuditEntry> getAllEntries(Organisation org) {
        if (!org.isCentralAuthority()) {
            throw new IllegalArgumentException("Only the Central Authority can view audit logs");
        }
        return auditRepository.loadAll();
    }

    public List<AuditEntry> getEntriesByDigitalId(Organisation org, String digitalIdId) {
        if (!org.isCentralAuthority()) {
            throw new IllegalArgumentException("Only the Central Authority can view audit logs");
        }
        return auditRepository.loadAll().stream()
            .filter(e -> e.getDigitalIdId().equals(digitalIdId))
            .toList();
    }

    public List<AuditEntry> getStatusHistory(Organisation org, String digitalIdId,
                                              LocalDate startDate, LocalDate endDate) {
        if (!org.isCentralAuthority()) {
            throw new IllegalArgumentException("Only the Central Authority can view audit logs");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }
        return auditRepository.loadAll().stream()
                .filter(e -> e.getEventType() == AuditEventType.STATUS_CHANGE)
                .filter(e -> e.getDigitalIdId().equals(digitalIdId))
                .filter(e -> {
                    LocalDate entryDate = e.getTimestamp().toLocalDate();
                    return !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate);
                })
                .toList();
    }
}
