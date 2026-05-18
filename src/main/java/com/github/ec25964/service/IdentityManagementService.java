package com.github.ec25964.service;

import com.github.ec25964.exception.AuthorisationException;
import com.github.ec25964.exception.IllegalTransitionException;
import com.github.ec25964.exception.NotFoundException;
import com.github.ec25964.exception.ValidationException;
import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.persistence.DigitalIdRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IdentityManagementService {

    private static final List<String> REQUIRED_ATTRIBUTES = List.of(
            "firstName", "lastName", "dateOfBirth", "address", "nationality", "email"
    );

    private final DigitalIdRepository repository;
    private final AuditService auditService;

    public IdentityManagementService(DigitalIdRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    public DigitalId create(Organisation org, Map<String, String> attributes) {
        if (!org.isCentralAuthority()) {
            throw new AuthorisationException(
                    "Only the Central Authority can create Digital IDs");
        }

        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_ATTRIBUTES) {
            String value = attributes.get(required);
            if (value == null || value.isBlank()) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new ValidationException(
                    "Missing required attributes: " + String.join(", ", missing));
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(attributes.get("dateOfBirth"));
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                "Invalid dateOfBirth: must be an ISO-format date (yyyy-MM-dd). " +
                "If called from a UI layer, convert before calling.");
        }

        validateDateOfBirthNotInFuture(dateOfBirth);
        validateEmailFormat(attributes.get("email"));

        DigitalId digitalId = DigitalId.create(
                attributes.get("firstName"),
                attributes.get("lastName"),
                dateOfBirth,
                attributes.get("address"),
                attributes.get("nationality"),
                attributes.get("email")
        );

        repository.save(digitalId);

        auditService.log(new AuditEntry(
                AuditEventType.CREATION,
                digitalId.getId(),
                org.getName(),
                LocalDateTime.now(),
                "Created new Digital ID"
        ));

        return digitalId;
    }

    public DigitalId updateAttribute(Organisation org, String digitalIdId,
                                     String attributeName, String newValue) {
        if (!org.isCentralAuthority()) {
            throw new AuthorisationException(
                    "Only the Central Authority can update Digital IDs");
        }

        DigitalId digitalId = repository.findById(digitalIdId)
                .orElseThrow(() -> new NotFoundException(
                        "Digital ID not found: " + digitalIdId));

        if (digitalId.getStatus() == IdStatus.REVOKED) {
            throw new IllegalTransitionException(
                    "Cannot update attributes of a REVOKED Digital ID");
        }

        if (newValue == null || newValue.isBlank()) {
            throw new ValidationException(
                    "New value for " + attributeName + " must not be blank");
        }

        String oldValue = applyUpdate(digitalId, attributeName, newValue);

        repository.save(digitalId);

        auditService.log(new AuditEntry(
                AuditEventType.UPDATE,
                digitalId.getId(),
                org.getName(),
                LocalDateTime.now(),
                attributeName + ": " + oldValue + " -> " + newValue
        ));

        return digitalId;
    }

    private String applyUpdate(DigitalId digitalId, String attributeName, String newValue) {
        return switch (attributeName) {
            case "firstName" -> {
                String old = digitalId.getFirstName();
                digitalId.setFirstName(newValue);
                yield old;
            }
            case "lastName" -> {
                String old = digitalId.getLastName();
                digitalId.setLastName(newValue);
                yield old;
            }
            case "address" -> {
                String old = digitalId.getAddress();
                digitalId.setAddress(newValue);
                yield old;
            }
            case "nationality" -> {
                String old = digitalId.getNationality();
                digitalId.setNationality(newValue);
                yield old;
            }
            case "email" -> {
                validateEmailFormat(newValue);
                String old = digitalId.getEmail();
                digitalId.setEmail(newValue);
                yield old;
            }
            case "id", "dateOfBirth" -> throw new IllegalTransitionException(
                    "Attribute '" + attributeName + "' is immutable and cannot be updated");
            default -> throw new ValidationException(
                    "Unknown attribute: " + attributeName);
        };
    }

    public DigitalId changeStatus(Organisation org, String digitalIdId,
                                  IdStatus newStatus, String reason) {
        if (!org.isCentralAuthority()) {
            throw new AuthorisationException(
                    "Only the Central Authority can change Digital ID status");
        }

        DigitalId digitalId = repository.findById(digitalIdId)
                .orElseThrow(() -> new NotFoundException(
                        "Digital ID not found: " + digitalIdId));

        IdStatus oldStatus = digitalId.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalTransitionException(
                    "Invalid status transition: " + oldStatus + " -> " + newStatus);
        }

        boolean reasonRequired = newStatus == IdStatus.SUSPENDED
                || newStatus == IdStatus.REVOKED;
        if (reasonRequired && (reason == null || reason.isBlank())) {
            throw new ValidationException(
                    "A reason is required when changing status to " + newStatus);
        }

        digitalId.setStatus(newStatus);
        repository.save(digitalId);

        String details = oldStatus + " -> " + newStatus;
        if (reason != null && !reason.isBlank()) {
            details += " | Reason: " + reason;
        }

        auditService.log(new AuditEntry(
                AuditEventType.STATUS_CHANGE,
                digitalId.getId(),
                org.getName(),
                LocalDateTime.now(),
                details
        ));

        return digitalId;
    }

    private void validateEmailFormat(String email) {
        if (email == null) {
            throw new ValidationException("Email must not be null");
        }
        int at = email.indexOf('@');
        if (at <= 0 || at != email.lastIndexOf('@')) {
            throw new ValidationException(
                    "Invalid email '" + email + "': must contain a single '@'");
        }
        String domain = email.substring(at + 1);
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            throw new ValidationException(
                    "Invalid email '" + email + "': domain must contain a '.'");
        }
    }

    private void validateDateOfBirthNotInFuture(LocalDate dateOfBirth) {
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new ValidationException(
                    "Invalid dateOfBirth: must not be in the future");
        }
    }

    public Optional<DigitalId> findById(String digitalIdId) {
        return repository.findById(digitalIdId);
    }

}
