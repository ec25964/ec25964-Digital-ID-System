package com.github.ec25964.service;

import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.OrganisationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OrganisationRegistryTest {

    private OrganisationRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new OrganisationRegistry();
    }

    @Test
    void registryContainsCentralAuthority() {
        long centralCount = registry.getAll().stream()
                .filter(o -> o.getType() == OrganisationType.CENTRAL_AUTHORITY)
                .count();
        assertEquals(1, centralCount);
    }

    @Test
    void registryContainsMultipleConsumingOrganisations() {
        long consumingCount = registry.getAll().stream()
                .filter(o -> o.getType() == OrganisationType.CONSUMING_ORGANISATION)
                .count();
        assertTrue(consumingCount >= 2);
    }

    @Test
    void findByNameReturnsCentralAuthority() {
        Optional<Organisation> org = registry.findByName("CentralAuthority");
        assertTrue(org.isPresent());
        assertTrue(org.get().isCentralAuthority());
    }

    @Test
    void findByNameReturnsConsumingOrg() {
        Optional<Organisation> org = registry.findByName("Employer");
        assertTrue(org.isPresent());
        assertFalse(org.get().isCentralAuthority());
    }

    @Test
    void findByNameReturnsEmptyForUnknown() {
        Optional<Organisation> org = registry.findByName("NonExistent");
        assertFalse(org.isPresent());
    }

    @Test
    void centralAuthorityHasAllAttributes() {
        Organisation central = registry.findByName("CentralAuthority").get();
        assertTrue(central.getAccessibleAttributes().contains("id"));
        assertTrue(central.getAccessibleAttributes().contains("firstName"));
        assertTrue(central.getAccessibleAttributes().contains("dateOfBirth"));
        assertTrue(central.getAccessibleAttributes().contains("email"));
        assertTrue(central.getAccessibleAttributes().contains("status"));
    }

    @Test
    void employerHasMinimalAttributes() {
        Organisation employer = registry.findByName("Employer").get();
        assertTrue(employer.getAccessibleAttributes().contains("id"));
        assertTrue(employer.getAccessibleAttributes().contains("firstName"));
        assertTrue(employer.getAccessibleAttributes().contains("lastName"));
        assertEquals(3, employer.getAccessibleAttributes().size());
    }

    @Test
    void foodBankHasOnlyId() {
        Organisation foodBank = registry.findByName("CommunityFoodBank").get();
        assertTrue(foodBank.getAccessibleAttributes().contains("id"));
        assertEquals(1, foodBank.getAccessibleAttributes().size());
    }

    @Test
    void differentOrgsHaveDifferentAccessLevels() {
        Organisation tax = registry.findByName("TaxAuthority").get();
        Organisation police = registry.findByName("ImmigrationService").get();
        Organisation employer = registry.findByName("Employer").get();

        assertNotEquals(tax.getAccessibleAttributes(), police.getAccessibleAttributes());
        assertNotEquals(tax.getAccessibleAttributes(), employer.getAccessibleAttributes());
        assertNotEquals(police.getAccessibleAttributes(), employer.getAccessibleAttributes());
    }
}
