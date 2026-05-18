package com.github.ec25964.service;

import com.github.ec25964.exception.AuthorisationException;
import com.github.ec25964.exception.NotFoundException;
import com.github.ec25964.exception.ValidationException;
import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.PeriodVerificationResult;
import com.github.ec25964.model.VerificationResult;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.DigitalIdRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IdentityVerificationService {

    private final DigitalIdRepository repository;
    private final AuditService auditService;
    private final AuditRepository auditRepository;

    public IdentityVerificationService(DigitalIdRepository repository,
                                       AuditService auditService,
                                       AuditRepository auditRepository) {
        this.repository = repository;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
    }

    public VerificationResult verify(Organisation org, String digitalIdId) {
        DigitalId digitalId = repository.findById(digitalIdId)
                .orElseThrow(() -> new NotFoundException(
                        "Digital ID not found: " + digitalIdId));

        Set<String> permitted = org.getAccessibleAttributes();
        Map<String, String> attributes = new LinkedHashMap<>();

        if (permitted.contains("firstName")) {
            attributes.put("firstName", digitalId.getFirstName());
        }
        if (permitted.contains("lastName")) {
            attributes.put("lastName", digitalId.getLastName());
        }
        if (permitted.contains("dateOfBirth")) {
            attributes.put("dateOfBirth", digitalId.getDateOfBirth().toString());
        }
        if (permitted.contains("address")) {
            attributes.put("address", digitalId.getAddress());
        }
        if (permitted.contains("nationality")) {
            attributes.put("nationality", digitalId.getNationality());
        }
        if (permitted.contains("email")) {
            attributes.put("email", digitalId.getEmail());
        }

        auditService.log(new AuditEntry(
                AuditEventType.VERIFICATION,
                digitalId.getId(),
                org.getName(),
                LocalDateTime.now(),
                "Verified by " + org.getName()
        ));

        return new VerificationResult(digitalId.getId(), digitalId.getStatus(), attributes);
    }

    public PeriodVerificationResult verifyContinuousActivity(
            Organisation org, String digitalIdId,
            LocalDate startDate, LocalDate endDate) {

        if (!org.canVerifyAcrossPeriod()) {
            throw new AuthorisationException(
                    "Organisation '" + org.getName()
                            + "' is not authorised to perform period verification");
        }

        if (startDate.isAfter(endDate)) {
            throw new ValidationException(
                    "Start date must not be after end date");
        }

        DigitalId digitalId = repository.findById(digitalIdId)
                .orElseThrow(() -> new NotFoundException(
                        "Digital ID not found: " + digitalIdId));

        List<AuditEntry> all = auditRepository.loadAll();

        List<AuditEntry> idStatusEvents = all.stream()
                .filter(e -> e.getEventType() == AuditEventType.STATUS_CHANGE)
                .filter(e -> e.getDigitalIdId().equals(digitalIdId))
                .toList();

        IdStatus statusAtStart = IdStatus.ACTIVE;
        for (AuditEntry e : idStatusEvents) {
            if (e.getTimestamp().toLocalDate().isAfter(startDate)) {
                break;
            }
            statusAtStart = parseNewStatus(e);
        }

        List<AuditEntry> inPeriod = idStatusEvents.stream()
                .filter(e -> {
                    LocalDate d = e.getTimestamp().toLocalDate();
                    return !d.isBefore(startDate) && !d.isAfter(endDate);
                })
                .toList();

        boolean leftActiveDuringPeriod = inPeriod.stream()
                .anyMatch(e -> parseNewStatus(e) != IdStatus.ACTIVE);

        boolean continuouslyActive =
                statusAtStart == IdStatus.ACTIVE && !leftActiveDuringPeriod;

        auditService.log(new AuditEntry(
                AuditEventType.VERIFICATION,
                digitalId.getId(),
                org.getName(),
                LocalDateTime.now(),
                "Period verification " + startDate + " to " + endDate
                        + ": " + (continuouslyActive ? "active throughout" : "not continuously active")
        ));

        return new PeriodVerificationResult(
                digitalIdId, startDate, endDate, continuouslyActive, inPeriod);
    }

    private IdStatus parseNewStatus(AuditEntry entry) {
        String details = entry.getDetails();
        int arrowIdx = details.indexOf("->");
        if (arrowIdx < 0) {
            return IdStatus.ACTIVE;
        }
        String afterArrow = details.substring(arrowIdx + 2).trim();
        int spaceIdx = afterArrow.indexOf(' ');
        String statusToken = (spaceIdx < 0) ? afterArrow : afterArrow.substring(0, spaceIdx);
        try {
            return IdStatus.valueOf(statusToken);
        } catch (IllegalArgumentException e) {
            return IdStatus.ACTIVE;
        }
    }
}
