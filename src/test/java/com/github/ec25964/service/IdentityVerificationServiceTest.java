package com.github.ec25964.service;

import com.github.ec25964.exception.AuthorisationException;
import com.github.ec25964.exception.NotFoundException;
import com.github.ec25964.exception.ValidationException;
import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;
import com.github.ec25964.model.PeriodVerificationResult;
import com.github.ec25964.model.VerificationResult;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class IdentityVerificationServiceTest {

    @TempDir
    Path tempDir;

    private IdentityManagementService managementService;
    private IdentityVerificationService verificationService;
    private AuditRepository auditRepo;
    private Organisation centralAuthority;
    private Organisation consumingOrg;
    private Organisation periodVerifyingOrg;

    @BeforeEach
    void setUp() {
        CsvParser parser = new CsvParser();
        CsvWriter writer = new CsvWriter();

        DigitalIdRepository idRepo =
                new DigitalIdRepository(tempDir.resolve("ids.csv"), parser, writer);
        auditRepo = new AuditRepository(tempDir.resolve("audit.csv"), parser, writer);

        AuditService auditService = new AuditService(auditRepo);
        managementService = new IdentityManagementService(idRepo, auditService);
        verificationService = new IdentityVerificationService(idRepo, auditService, auditRepo);

        centralAuthority = new Organisation("CentralAuthority",
                OrganisationType.CENTRAL_AUTHORITY,
                Set.of("id", "firstName", "lastName", "dateOfBirth",
                        "address", "nationality", "email", "status"),
                true);
        consumingOrg = new Organisation("Bank",
                OrganisationType.CONSUMING_ORGANISATION,
                Set.of("id", "firstName", "lastName", "dateOfBirth"),
                false);
        periodVerifyingOrg = new Organisation("TaxAuthority",
                OrganisationType.CONSUMING_ORGANISATION,
                Set.of("id", "firstName", "lastName", "address", "nationality"),
                true);
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

    // ---------- verify ----------

    @Test
    void verifyByCentralAuthorityReturnsAllAttributes() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        VerificationResult result = verificationService.verify(centralAuthority, created.getId());

        assertEquals(created.getId(), result.getDigitalIdId());
        assertEquals(IdStatus.ACTIVE, result.getStatus());
        assertEquals("John", result.getAttributes().get("firstName"));
        assertEquals("Doe", result.getAttributes().get("lastName"));
        assertEquals("1990-05-15", result.getAttributes().get("dateOfBirth"));
        assertEquals("123 Main St", result.getAttributes().get("address"));
        assertEquals("British", result.getAttributes().get("nationality"));
        assertEquals("john@example.com", result.getAttributes().get("email"));
    }

    @Test
    void verifyByConsumingOrgReturnsOnlyPermittedAttributes() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());
        VerificationResult result = verificationService.verify(consumingOrg, created.getId());

        assertTrue(result.getAttributes().containsKey("firstName"));
        assertTrue(result.getAttributes().containsKey("lastName"));
        assertTrue(result.getAttributes().containsKey("dateOfBirth"));
        assertFalse(result.getAttributes().containsKey("address"));
        assertFalse(result.getAttributes().containsKey("nationality"));
        assertFalse(result.getAttributes().containsKey("email"));
    }

    @Test
    void verifyAlwaysIncludesStatus() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());
        Organisation minimalOrg = new Organisation("MinimalOrg",
                OrganisationType.CONSUMING_ORGANISATION, Set.of("id"), false);

        VerificationResult result = verificationService.verify(minimalOrg, created.getId());

        assertEquals(IdStatus.ACTIVE, result.getStatus());
        assertEquals(created.getId(), result.getDigitalIdId());
        assertTrue(result.getAttributes().isEmpty());
    }

    @Test
    void verifyReflectsCurrentStatus() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());
        managementService.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Test");

        VerificationResult result = verificationService.verify(consumingOrg, created.getId());

        assertEquals(IdStatus.SUSPENDED, result.getStatus());
    }

    @Test
    void verifyRejectsNonExistentId() {
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> verificationService.verify(consumingOrg, "does-not-exist"));
        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void verifyLogsAuditEvent() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        verificationService.verify(consumingOrg, created.getId());

        AuditEntry verifyEntry = auditRepo.loadAll().stream()
                .filter(e -> e.getEventType() == AuditEventType.VERIFICATION)
                .findFirst()
                .orElseThrow();

        assertEquals(created.getId(), verifyEntry.getDigitalIdId());
        assertEquals("Bank", verifyEntry.getOrganisation());
    }

    // ---------- verifyContinuousActivity ----------

    @Test
    void verifyContinuousActivityReturnsTrueForUntouchedActiveId() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        PeriodVerificationResult result = verificationService.verifyContinuousActivity(
                periodVerifyingOrg, created.getId(),
                LocalDate.now().minusDays(7), LocalDate.now());

        assertTrue(result.isContinuouslyActive());
        assertTrue(result.getStatusEventsInPeriod().isEmpty());
    }

    @Test
    void verifyContinuousActivityReturnsFalseWhenSuspendedDuringPeriod() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());
        managementService.changeStatus(centralAuthority, created.getId(),
                IdStatus.SUSPENDED, "Test");

        PeriodVerificationResult result = verificationService.verifyContinuousActivity(
                periodVerifyingOrg, created.getId(),
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        assertFalse(result.isContinuouslyActive());
        assertEquals(1, result.getStatusEventsInPeriod().size());
    }

    @Test
    void verifyContinuousActivityRejectsOrgWithoutPermission() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        AuthorisationException ex = assertThrows(AuthorisationException.class,
                () -> verificationService.verifyContinuousActivity(consumingOrg, created.getId(),
                        LocalDate.now().minusDays(7), LocalDate.now()));
        assertTrue(ex.getMessage().toLowerCase().contains("not authorised"));
    }

    @Test
    void verifyContinuousActivityRejectsStartAfterEnd() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        assertThrows(ValidationException.class,
                () -> verificationService.verifyContinuousActivity(periodVerifyingOrg, created.getId(),
                        LocalDate.of(2024, 6, 1), LocalDate.of(2024, 1, 1)));
    }

    @Test
    void verifyContinuousActivityRejectsNonExistentId() {
        assertThrows(NotFoundException.class,
                () -> verificationService.verifyContinuousActivity(periodVerifyingOrg, "no-such-id",
                        LocalDate.now().minusDays(7), LocalDate.now()));
    }

    @Test
    void verifyContinuousActivityLogsAuditEvent() {
        DigitalId created = managementService.create(centralAuthority, validAttributes());

        verificationService.verifyContinuousActivity(periodVerifyingOrg, created.getId(),
                LocalDate.now().minusDays(7), LocalDate.now());

        boolean found = auditRepo.loadAll().stream()
                .anyMatch(e -> e.getEventType() == AuditEventType.VERIFICATION
                        && e.getDetails().contains("Period verification"));
        assertTrue(found);
    }
}
