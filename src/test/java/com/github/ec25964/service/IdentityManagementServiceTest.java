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
import com.github.ec25964.model.OrganisationType;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.CsvParser;
import com.github.ec25964.persistence.CsvWriter;
import com.github.ec25964.persistence.DigitalIdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdentityManagementServiceTest {

    @TempDir
    Path tempDir;

    private IdentityManagementService service;
    private DigitalIdRepository idRepo;
    private AuditRepository auditRepo;
    private Organisation centralAuthority;
    private Organisation consumingOrg;

    @BeforeEach
    void setUp() {
        CsvParser parser = new CsvParser();
        CsvWriter writer = new CsvWriter();

        idRepo = new DigitalIdRepository(tempDir.resolve("ids.csv"), parser, writer);
        auditRepo = new AuditRepository(tempDir.resolve("audit.csv"), parser, writer);

        AuditService auditService = new AuditService(auditRepo);
        service = new IdentityManagementService(idRepo, auditService);

        centralAuthority = new Organisation("CentralAuthority",
                OrganisationType.CENTRAL_AUTHORITY,
                Set.of("id", "firstName", "lastName", "dateOfBirth",
                        "address", "nationality", "email", "status"),
                true);
        consumingOrg = new Organisation("Bank",
                OrganisationType.CONSUMING_ORGANISATION,
                Set.of("id", "firstName", "lastName", "dateOfBirth"),
                false);
    }

    private Map<String, String> validAttributes() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("firstName", "John");
        attrs.put("lastName", "Doe");
        attrs.put("dateOfBirth", "1990-05-15");
        attrs.put("address", "123 Main St");
        attrs.put("nationality", "British");
        attrs.put("email", "john@example.com");
        return attrs;
    }

    // ---------- create ----------

    @Test
    void createByCentralAuthorityReturnsActiveDigitalIdWithGeneratedId() {
        DigitalId result = service.create(centralAuthority, validAttributes());

        assertNotNull(result.getId());
        assertFalse(result.getId().isBlank());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(IdStatus.ACTIVE, result.getStatus());
    }

    @Test
    void createPersistsToRepository() {
        DigitalId result = service.create(centralAuthority, validAttributes());

        Optional<DigitalId> loaded = idRepo.findById(result.getId());
        assertTrue(loaded.isPresent());
        assertEquals("John", loaded.get().getFirstName());
    }

    @Test
    void createLogsAuditEvent() {
        DigitalId result = service.create(centralAuthority, validAttributes());

        List<AuditEntry> entries = auditRepo.loadAll();
        assertEquals(1, entries.size());
        AuditEntry entry = entries.get(0);
        assertEquals(AuditEventType.CREATION, entry.getEventType());
        assertEquals(result.getId(), entry.getDigitalIdId());
        assertEquals("CentralAuthority", entry.getOrganisation());
    }

    @Test
    void createRejectsConsumingOrganisation() {
        assertThrows(AuthorisationException.class,
                () -> service.create(consumingOrg, validAttributes()));
    }

    @Test
    void createWithMissingAttributesListsMissingFields() {
        Map<String, String> incomplete = new HashMap<>();
        incomplete.put("firstName", "John");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, incomplete));

        String message = ex.getMessage();
        assertTrue(message.contains("lastName"));
        assertTrue(message.contains("dateOfBirth"));
        assertTrue(message.contains("address"));
    }

    @Test
    void createWithBlankAttributeIsRejected() {
        Map<String, String> attrs = validAttributes();
        attrs.put("firstName", "   ");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().contains("firstName"));
    }

    @Test
    void createWithInvalidDateFormatIsRejected() {
        Map<String, String> attrs = validAttributes();
        attrs.put("dateOfBirth", "not-a-date");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().toLowerCase().contains("dateofbirth"));
    }

    @Test
    void createRejectsEmailWithoutAtSign() {
        Map<String, String> attrs = validAttributes();
        attrs.put("email", "no-at-sign.com");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void createRejectsEmailWithoutDomainDot() {
        Map<String, String> attrs = validAttributes();
        attrs.put("email", "user@nodomain");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void createRejectsEmailWithMultipleAtSigns() {
        Map<String, String> attrs = validAttributes();
        attrs.put("email", "user@host@example.com");

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    @Test
    void createRejectsFutureDateOfBirth() {
        Map<String, String> attrs = validAttributes();
        attrs.put("dateOfBirth", LocalDate.now().plusDays(1).toString());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.create(centralAuthority, attrs));
        assertTrue(ex.getMessage().toLowerCase().contains("future"));
    }

    @Test
    void createAcceptsTodaysDateOfBirth() {
        Map<String, String> attrs = validAttributes();
        attrs.put("dateOfBirth", LocalDate.now().toString());

        DigitalId result = service.create(centralAuthority, attrs);
        assertEquals(LocalDate.now(), result.getDateOfBirth());
    }

    // ---------- updateAttribute ----------

    @Test
    void updateAttributeChangesValueAndPersists() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        service.updateAttribute(centralAuthority, created.getId(), "firstName", "Jane");

        DigitalId loaded = idRepo.findById(created.getId()).orElseThrow();
        assertEquals("Jane", loaded.getFirstName());
    }

    @Test
    void updateAttributeLogsAuditEventWithOldAndNewValue() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        service.updateAttribute(centralAuthority, created.getId(), "address", "456 New Rd");

        List<AuditEntry> entries = auditRepo.loadAll();
        AuditEntry updateEntry = entries.stream()
                .filter(e -> e.getEventType() == AuditEventType.UPDATE)
                .findFirst()
                .orElseThrow();

        assertEquals(created.getId(), updateEntry.getDigitalIdId());
        assertEquals("CentralAuthority", updateEntry.getOrganisation());
        assertTrue(updateEntry.getDetails().contains("address"));
        assertTrue(updateEntry.getDetails().contains("123 Main St"));
        assertTrue(updateEntry.getDetails().contains("456 New Rd"));
    }

    @Test
    void updateAttributeRejectsConsumingOrganisation() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        assertThrows(AuthorisationException.class, () ->
                service.updateAttribute(consumingOrg, created.getId(), "firstName", "Jane"));
    }

    @Test
    void updateAttributeRejectsImmutableIdField() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class, () ->
                service.updateAttribute(centralAuthority, created.getId(), "id", "new-id"));
        assertTrue(ex.getMessage().toLowerCase().contains("immutable"));
    }

    @Test
    void updateAttributeRejectsImmutableDateOfBirthField() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class, () ->
                service.updateAttribute(centralAuthority, created.getId(),
                        "dateOfBirth", "2000-01-01"));
        assertTrue(ex.getMessage().toLowerCase().contains("immutable"));
    }

    @Test
    void updateAttributeRejectsNonExistentId() {
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.updateAttribute(centralAuthority, "does-not-exist",
                        "firstName", "Jane"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void updateAttributeRejectsUnknownAttribute() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.updateAttribute(centralAuthority, created.getId(),
                        "favouriteColour", "blue"));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown"));
    }

    @Test
    void updateAttributeRejectsBlankValue() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        assertThrows(ValidationException.class, () ->
                service.updateAttribute(centralAuthority, created.getId(),
                        "firstName", "   "));
    }

    @Test
    void updateAttributeOnRevokedIdIsRejected() {
        DigitalId created = service.create(centralAuthority, validAttributes());
        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.REVOKED, "Confirmed fraud");

        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class,
                () -> service.updateAttribute(centralAuthority, created.getId(),
                        "firstName", "Jane"));
        assertTrue(ex.getMessage().toLowerCase().contains("revoked"));
    }

    @Test
    void updateAttributeOnSuspendedIdStillSucceeds() {
        DigitalId created = service.create(centralAuthority, validAttributes());
        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Fraud investigation");

        DigitalId updated = service.updateAttribute(centralAuthority,
                created.getId(), "email", "new@example.com");

        assertEquals("new@example.com", updated.getEmail());
    }

    @Test
    void updateAttributeOnActiveIdStillSucceeds() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        DigitalId updated = service.updateAttribute(centralAuthority,
                created.getId(), "firstName", "Jane");

        assertEquals("Jane", updated.getFirstName());
    }

    @Test
    void updateAttributeRejectsInvalidEmail() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.updateAttribute(centralAuthority, created.getId(),
                        "email", "not-valid"));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
    }

    // ---------- changeStatus ----------

    @Test
    void changeStatusFromActiveToSuspendedWithReasonSucceeds() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        DigitalId updated = service.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Suspected fraud");

        assertEquals(IdStatus.SUSPENDED, updated.getStatus());
        assertEquals(IdStatus.SUSPENDED,
                idRepo.findById(created.getId()).orElseThrow().getStatus());
    }

    @Test
    void changeStatusFromSuspendedToActiveWithoutReasonSucceeds() {
        DigitalId created = service.create(centralAuthority, validAttributes());
        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Fraud check");

        DigitalId updated = service.changeStatus(centralAuthority, created.getId(),
                IdStatus.ACTIVE, null);

        assertEquals(IdStatus.ACTIVE, updated.getStatus());
    }

    @Test
    void changeStatusToSuspendedWithoutReasonIsRejected() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.changeStatus(centralAuthority, created.getId(),
                        IdStatus.SUSPENDED, null));
        assertTrue(ex.getMessage().toLowerCase().contains("reason"));
    }

    @Test
    void changeStatusToRevokedWithoutReasonIsRejected() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.changeStatus(centralAuthority, created.getId(),
                        IdStatus.REVOKED, "   "));
        assertTrue(ex.getMessage().toLowerCase().contains("reason"));
    }

    @Test
    void changeStatusRejectsInvalidTransition() {
        DigitalId created = service.create(centralAuthority, validAttributes());
        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.REVOKED, "Final action");

        IllegalTransitionException ex = assertThrows(IllegalTransitionException.class, () ->
                service.changeStatus(centralAuthority, created.getId(),
                        IdStatus.ACTIVE, null));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void changeStatusRejectsConsumingOrganisation() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        assertThrows(AuthorisationException.class, () ->
                service.changeStatus(consumingOrg, created.getId(),
                        IdStatus.SUSPENDED, "Test"));
    }

    @Test
    void changeStatusRejectsNonExistentId() {
        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.changeStatus(centralAuthority, "does-not-exist",
                        IdStatus.SUSPENDED, "Test"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void changeStatusAuditEntryIncludesReasonWhenProvided() {
        DigitalId created = service.create(centralAuthority, validAttributes());

        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Suspected identity fraud");

        AuditEntry statusEntry = auditRepo.loadAll().stream()
                .filter(e -> e.getEventType() == AuditEventType.STATUS_CHANGE)
                .findFirst()
                .orElseThrow();

        assertTrue(statusEntry.getDetails().contains("ACTIVE"));
        assertTrue(statusEntry.getDetails().contains("SUSPENDED"));
        assertTrue(statusEntry.getDetails().contains("Suspected identity fraud"));
    }

    @Test
    void changeStatusAuditEntryOmitsReasonWhenNotProvided() {
        DigitalId created = service.create(centralAuthority, validAttributes());
        service.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Initial suspension");

        service.changeStatus(centralAuthority, created.getId(), IdStatus.ACTIVE, null);

        List<AuditEntry> statusEntries = auditRepo.loadAll().stream()
                .filter(e -> e.getEventType() == AuditEventType.STATUS_CHANGE)
                .toList();
        AuditEntry reactivation = statusEntries.get(statusEntries.size() - 1);

        assertTrue(reactivation.getDetails().contains("SUSPENDED"));
        assertTrue(reactivation.getDetails().contains("ACTIVE"));
        assertFalse(reactivation.getDetails().toLowerCase().contains("reason"));
    }
}
