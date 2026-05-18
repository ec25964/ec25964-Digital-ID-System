package com.github.ec25964.cli;

import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.CsvParser;
import com.github.ec25964.persistence.CsvWriter;
import com.github.ec25964.persistence.DigitalIdRepository;
import com.github.ec25964.service.AuditService;
import com.github.ec25964.service.DigitalIdService;
import com.github.ec25964.service.OrganisationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class CliControllerTest {

    @TempDir
    Path tempDir;

    private DigitalIdRepository idRepo;
    private DigitalIdService digitalIdService;
    private AuditService auditService;
    private OrganisationRegistry registry;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {
        CsvParser parser = new CsvParser();
        CsvWriter writer = new CsvWriter();
        idRepo = new DigitalIdRepository(tempDir.resolve("ids.csv"), parser, writer);
        AuditRepository auditRepo = new AuditRepository(
                tempDir.resolve("audit.csv"), parser, writer);
        auditService = new AuditService(auditRepo);
        digitalIdService = new DigitalIdService(idRepo, auditService, auditRepo);
        registry = new OrganisationRegistry();
        capturedOut = new ByteArrayOutputStream();
    }

    private String runCliWith(String input) {
        Scanner scanner = new Scanner(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        PrintStream out = new PrintStream(capturedOut, true, StandardCharsets.UTF_8);

        CliController controller = new CliController(
                digitalIdService, auditService, registry, scanner, out);
        controller.start();
        out.flush();
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    @Test
    void invalidOrganisationChoiceRePromptsUntilValid() {
        String output = runCliWith("99\n1\n0\n");

        assertTrue(output.contains("Invalid choice"));
        assertTrue(output.contains("Operating as: CentralAuthority"));
    }

    @Test
    void nonNumericInputAtMenuRePrompts() {
        String output = runCliWith("not-a-number\n1\n0\n");

        assertTrue(output.contains("Please enter a number"));
        assertTrue(output.contains("Operating as: CentralAuthority"));
    }

    @Test
    void eofTerminatesGracefully() {
        String output = runCliWith("");

        assertTrue(output.contains("Select your organisation"));
    }

    @Test
    void consumingOrganisationSeesLimitedCommandMenu() {
        String output = runCliWith("2\n0\n");

        assertTrue(output.contains("Operating as: TaxAuthority"));
        assertTrue(output.contains("Verify Digital ID"));
        assertFalse(output.contains("Create Digital ID"));
    }

    @Test
    void exitCommandShowsGoodbyeAndTerminates() {
        String output = runCliWith("1\n0\n");

        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void createCommandPromptsForFieldsPersistsAndShowsId() {
        String input = String.join("\n",
                "1",
                "1",
                "John",
                "Doe",
                "15/05/1990",
                "123 Main St",
                "British",
                "john@example.com",
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.contains("Created Digital ID:"));
        assertEquals(1, idRepo.loadAll().size());

        DigitalId stored = idRepo.loadAll().get(0);
        assertEquals("John", stored.getFirstName());
        assertEquals(LocalDate.of(1990, 5, 15), stored.getDateOfBirth());
        assertEquals(IdStatus.ACTIVE, stored.getStatus());
    }

    @Test
    void verifyCommandShowsUkDateFormatInOutput() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("firstName", "Alice");
        attrs.put("lastName", "Smith");
        attrs.put("dateOfBirth", "1992-03-20");
        attrs.put("address", "10 Demo Lane");
        attrs.put("nationality", "British");
        attrs.put("email", "alice@example.com");
        DigitalId created = digitalIdService.create(
                registry.findByName("CentralAuthority").orElseThrow(), attrs);

        String input = String.join("\n",
                "1",
                "4",
                created.getId(),
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.contains("Status: ACTIVE"));
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("20/03/1992"));
        assertFalse(output.contains("1992-03-20"));
    }

    @Test
    void serviceErrorsAreCaughtAndDisplayedAsErrorMessage() {
        String input = String.join("\n",
                "1",
                "3",
                "fake-id-does-not-exist",
                "SUSPENDED",
                "Test reason",
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.toLowerCase().contains("error"));
        assertTrue(output.toLowerCase().contains("not found"));
    }

    @Test
    void invalidDateInputRePromptsRatherThanCrashing() {
        String input = String.join("\n",
                "1",
                "1",
                "John",
                "Doe",
                "1990-05-15",
                "15/05/1990",
                "123 Main St",
                "British",
                "john@example.com",
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.contains("Invalid date"));
        assertTrue(output.contains("Created Digital ID:"));
        assertEquals(1, idRepo.loadAll().size());
    }

    @Test
    void updateAttributeOnRevokedIdFailsFastBeforeFurtherPrompts() {
        var ca = registry.findByName("CentralAuthority").orElseThrow();
        Map<String, String> attrs = new HashMap<>();
        attrs.put("firstName", "Test");
        attrs.put("lastName", "User");
        attrs.put("dateOfBirth", "1990-01-01");
        attrs.put("address", "1 Test St");
        attrs.put("nationality", "British");
        attrs.put("email", "test@example.com");
        DigitalId created = digitalIdService.create(ca, attrs);
        digitalIdService.changeStatus(ca, created.getId(),
                IdStatus.REVOKED, "Test revocation");


        String input = String.join("\n",
                "1",
                "2",
                created.getId(),
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.toLowerCase().contains("revoked"));
        assertFalse(output.contains("Attribute to update"));
    }

    @Test
    void verifyOnNonExistentIdFailsFastWithErrorMessage() {
        String input = String.join("\n",
                "2",
                "1",
                "no-such-id",
                "0",
                "");

        String output = runCliWith(input);

        assertTrue(output.toLowerCase().contains("not found"));
        assertFalse(output.contains("Verification of"));
    }

}
