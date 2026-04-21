package com.github.ec25964.persistence;

import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditRepositoryTest {

    @TempDir
    Path tempDir;

    private AuditRepository repo;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("audit.csv");
        repo = new AuditRepository(file, new CsvParser(), new CsvWriter());
    }

    @Test
    void loadAllFromEmptyReturnsEmptyList() {
        assertTrue(repo.loadAll().isEmpty());
    }

    @Test
    void appendAndLoadReturnsEntries() {
        AuditEntry entry = new AuditEntry(
                AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 30, 0), "Created new Digital ID"
        );

        repo.append(entry);
        List<AuditEntry> entries = repo.loadAll();

        assertEquals(1, entries.size());
        assertEquals(AuditEventType.CREATION, entries.get(0).getEventType());
        assertEquals("id-1", entries.get(0).getDigitalIdId());
        assertEquals("CentralAuthority", entries.get(0).getOrganisation());
    }

    @Test
    void appendPreservesOrder() {
        repo.append(new AuditEntry(AuditEventType.CREATION, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "First"));
        repo.append(new AuditEntry(AuditEventType.UPDATE, "id-1", "CentralAuthority",
                LocalDateTime.of(2024, 1, 16, 11, 0, 0), "Second"));

        List<AuditEntry> entries = repo.loadAll();

        assertEquals(2, entries.size());
        assertEquals("First", entries.get(0).getDetails());
        assertEquals("Second", entries.get(1).getDetails());
    }
}
