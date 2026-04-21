package com.github.ec25964.persistence;

import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DigitalIdRepositoryTest {

    @TempDir
    Path tempDir;

    private DigitalIdRepository repo;

    @BeforeEach
    void setUp() {
        Path file = tempDir.resolve("ids.csv");
        repo = new DigitalIdRepository(file, new CsvParser(), new CsvWriter());
    }

    @Test
    void loadAllFromEmptyReturnsEmptyList() {
        assertTrue(repo.loadAll().isEmpty());
    }

    @Test
    void saveAndFindById() {
        DigitalId id = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");

        repo.save(id);
        Optional<DigitalId> found = repo.findById(id.getId());

        assertTrue(found.isPresent());
        assertEquals("John", found.get().getFirstName());
        assertEquals("Doe", found.get().getLastName());
        assertEquals(LocalDate.of(1990, 5, 15), found.get().getDateOfBirth());
        assertEquals(IdStatus.ACTIVE, found.get().getStatus());
    }

    @Test
    void findByIdReturnsEmptyForMissingId() {
        Optional<DigitalId> found = repo.findById("nonexistent");
        assertFalse(found.isPresent());
    }

    @Test
    void saveUpdatesExistingRecord() {
        DigitalId id = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");

        repo.save(id);
        id.setFirstName("Jane");
        repo.save(id);

        assertEquals(1, repo.loadAll().size());
        assertEquals("Jane", repo.findById(id.getId()).get().getFirstName());
    }

    @Test
    void roundTripPreservesAllFields() {
        DigitalId original = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");

        repo.save(original);
        DigitalId loaded = repo.findById(original.getId()).get();

        assertEquals(original.getId(), loaded.getId());
        assertEquals(original.getFirstName(), loaded.getFirstName());
        assertEquals(original.getLastName(), loaded.getLastName());
        assertEquals(original.getDateOfBirth(), loaded.getDateOfBirth());
        assertEquals(original.getAddress(), loaded.getAddress());
        assertEquals(original.getNationality(), loaded.getNationality());
        assertEquals(original.getEmail(), loaded.getEmail());
        assertEquals(original.getStatus(), loaded.getStatus());
    }
}
