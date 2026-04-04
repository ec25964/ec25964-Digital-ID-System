package com.github.ec25964.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class DigitalIdTest {

    @Test
    void createGeneratesUniqueIdAndSetsActive() {
        DigitalId id = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");

        assertNotNull(id.getId());
        assertFalse(id.getId().isEmpty());
        assertEquals(IdStatus.ACTIVE, id.getStatus());
    }

    @Test
    void createGeneratesUniqueIds() {
        DigitalId id1 = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");
        DigitalId id2 = DigitalId.create("Jane", "Doe", LocalDate.of(1992, 3, 20),
                "456 Oak Ave", "British", "jane@example.com");

        assertNotEquals(id1.getId(), id2.getId());
    }

    @Test
    void immutableFieldsCannotChange() {
        DigitalId id = DigitalId.create("John", "Doe", LocalDate.of(1990, 5, 15),
                "123 Main St", "British", "john@example.com");

        assertEquals(LocalDate.of(1990, 5, 15), id.getDateOfBirth());
    }
}
