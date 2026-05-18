package com.github.ec25964.service;

import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.CsvParser;
import com.github.ec25964.persistence.CsvWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuditServiceTest {

    @TempDir
    Path tempDir;

    private AuditService auditService;
    private AuditRepository repo;
    private Organisation centralAuthority;
    private Organisation consumingOrg;

    @BeforeEach
    void setUp() {
        Path auditFile = tempDir.resolve("audit.csv");
        repo = new AuditRepository(auditFile, new CsvParser(), new CsvWriter());
        auditService = new AuditService(repo);

        centralAuthority = new Organisation("CentralAuthority",
                OrganisationType.CENTRAL_AUTHORITY, Set.of(), true);
        consumingOrg = new Organisation("Bank",
            OrganisationType.CONSUMING_ORGANISATION, Set.of(), false);
    }

    @Test
    void logAppendsEntryToRepository() {
        AuditEntry entry = new AuditEntry(
                AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "Created");

        auditService.log(entry);

        List<AuditEntry> stored = repo.loadAll();
        assertEquals(1, stored.size());
        assertEquals("id-1", stored.get(0).getDigitalIdId());
        assertEquals(AuditEventType.CREATION, stored.get(0).getEventType());
    }

    @Test
    void getAllEntriesReturnsAllForCentralAuthority() {
        auditService.log(new AuditEntry(AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "Created"));
        auditService.log(new AuditEntry(AuditEventType.VERIFICATION, "id-1", "Bank",
                LocalDateTime.of(2024, 1, 16, 11, 0, 0), "Verified"));

        List<AuditEntry> entries = auditService.getAllEntries(centralAuthority);

        assertEquals(2, entries.size());
    }

    @Test
    void getAllEntriesRejectsConsumingOrganisation() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.getAllEntries(consumingOrg));
    }

    @Test
    void getEntriesByDigitalIdReturnsOnlyMatchingEntries() {
        auditService.log(new AuditEntry(AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "Created id-1"));
        auditService.log(new AuditEntry(AuditEventType.CREATION, "id-2", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 5, 0), "Created id-2"));
        auditService.log(new AuditEntry(AuditEventType.VERIFICATION, "id-1", "Bank",
                LocalDateTime.of(2024, 1, 16, 11, 0, 0), "Verified id-1"));

        List<AuditEntry> entries = auditService.getEntriesByDigitalId(centralAuthority, "id-1");

        assertEquals(2, entries.size());
        assertTrue(entries.stream().allMatch(e -> e.getDigitalIdId().equals("id-1")));
    }

    @Test
    void getEntriesByDigitalIdRejectsConsumingOrganisation() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.getEntriesByDigitalId(consumingOrg, "id-1"));
    }

    @Test
    void getStatusHistoryReturnsOnlyStatusChangeEvents() {
        auditService.log(new AuditEntry(AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 10, 10, 0, 0), "Created"));
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "ACTIVE -> SUSPENDED | Reason: Fraud check"));
        auditService.log(new AuditEntry(AuditEventType.VERIFICATION, "id-1", "Bank",
                LocalDateTime.of(2024, 1, 16, 11, 0, 0), "Verified"));

        List<AuditEntry> history = auditService.getStatusHistory(
                centralAuthority, "id-1",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertEquals(1, history.size());
        assertEquals(AuditEventType.STATUS_CHANGE, history.get(0).getEventType());
    }

    @Test
    void getStatusHistoryFiltersByDateRangeInclusive() {
        // Entry on the start boundary — should be included
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 10, 0, 0, 0), "On start date"));
        // Entry in the middle — should be included
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 12, 0, 0), "Middle of range"));
        // Entry on the end boundary — should be included
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 20, 23, 59, 0), "On end date"));
        // Entry before the range — should be excluded
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 9, 23, 59, 0), "Before range"));
        // Entry after the range — should be excluded
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 21, 0, 0, 0), "After range"));

        List<AuditEntry> history = auditService.getStatusHistory(
                centralAuthority, "id-1",
                LocalDate.of(2024, 1, 10), LocalDate.of(2024, 1, 20));

        assertEquals(3, history.size());
    }

    @Test
    void getStatusHistoryReturnsEmptyListWhenNoMatches() {
        auditService.log(new AuditEntry(AuditEventType.STATUS_CHANGE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 6, 1, 10, 0, 0), "Outside query window"));

        List<AuditEntry> history = auditService.getStatusHistory(
                centralAuthority, "id-1",
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        assertTrue(history.isEmpty());
    }

    @Test
    void getStatusHistoryRejectsStartDateAfterEndDate() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.getStatusHistory(
                        centralAuthority, "id-1",
                        LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)));
    }

    @Test
    void getStatusHistoryRejectsConsumingOrganisation() {
        assertThrows(IllegalArgumentException.class,
                () -> auditService.getStatusHistory(
                        consumingOrg, "id-1",
                        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));
    }
}
