package com.github.ec25964.cli;

import com.github.ec25964.exception.DigitalIdException;
import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;
import com.github.ec25964.model.Organisation;
import com.github.ec25964.model.PeriodVerificationResult;
import com.github.ec25964.model.VerificationResult;
import com.github.ec25964.service.AuditService;
import com.github.ec25964.service.DigitalIdService;
import com.github.ec25964.service.OrganisationRegistry;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class CliController {

    private static final DateTimeFormatter UI_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DigitalIdService digitalIdService;
    private final AuditService auditService;
    private final OrganisationRegistry registry;
    private final MenuRenderer renderer;
    private final Scanner in;
    private final PrintStream out;

    public CliController(DigitalIdService digitalIdService,
                         AuditService auditService,
                         OrganisationRegistry registry,
                         Scanner in,
                         PrintStream out) {
        this.digitalIdService = digitalIdService;
        this.auditService = auditService;
        this.registry = registry;
        this.renderer = new MenuRenderer();
        this.in = in;
        this.out = out;
    }

    public void start() {
        Organisation selected = selectOrganisation();
        if (selected == null) {
            return;
        }
        runCommandLoop(selected);
        out.println("\nGoodbye.");
    }

    // ----- Menu loops -----

    private Organisation selectOrganisation() {
        List<Organisation> orgs = registry.getAll();

        while (true) {
            out.print(renderer.renderOrganisationMenu(orgs));
            Integer choice = readNumericChoice();
            if (choice == null) {
                return null;
            }
            if (choice >= 1 && choice <= orgs.size()) {
                return orgs.get(choice - 1);
            }
            out.println("Invalid choice. Please pick a number from the list.");
        }
    }

    private void runCommandLoop(Organisation org) {
        while (true) {
            out.print(renderer.renderCommandMenu(org));
            Integer choice = readNumericChoice();
            if (choice == null) {
                return;
            }
            if (choice == 0) {
                return;
            }
            handleCommand(org, choice);
        }
    }

    private void handleCommand(Organisation org, int choice) {
        try {
            if (org.isCentralAuthority()) {
                handleCentralAuthorityCommand(choice);
            } else {
                handleConsumingOrganisationCommand(org, choice);
            }
        } catch (DigitalIdException e) {
            out.println("Error: " + e.getMessage());
        }
    }

    private void handleCentralAuthorityCommand(int choice) {
        Organisation org = registry.findByName("CentralAuthority").orElseThrow();
        switch (choice) {
            case 1 -> handleCreate(org);
            case 2 -> handleUpdateAttribute(org);
            case 3 -> handleChangeStatus(org);
            case 4 -> handleVerify(org);
            case 5 -> handleViewAuditLogs(org);
            case 6 -> handleViewAuditLogsByDigitalId(org);
            case 7 -> handleStatusHistory(org);
            default -> out.println("Unknown choice. Try again.");
        }
    }

    private void handleConsumingOrganisationCommand(Organisation org, int choice) {
        switch (choice) {
            case 1 -> handleVerify(org);
            case 2 -> {
                if (org.canVerifyAcrossPeriod()) {
                    handlePeriodVerify(org);
                } else {
                    out.println("Unknown choice. Try again.");
                }
            }
            default -> out.println("Unknown choice. Try again.");
        }
    }

    // ----- Command handlers -----

    private void handleCreate(Organisation org) {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("firstName", prompt("First name: "));
        attrs.put("lastName", prompt("Last name: "));
        attrs.put("dateOfBirth", promptIsoDate("Date of birth (dd/MM/yyyy): "));
        attrs.put("address", prompt("Address: "));
        attrs.put("nationality", prompt("Nationality: "));
        attrs.put("email", prompt("Email: "));

        DigitalId created = digitalIdService.create(org, attrs);
        out.println();
        out.println("Created Digital ID: " + created.getId());
    }

    private void handleUpdateAttribute(Organisation org) {
        String id = prompt("Digital ID: ");
        if (preflightId(id, true).isEmpty()) {
            return;
        }
        String attribute = prompt("Attribute to update " +
                "(firstName, lastName, address, nationality, email): ");
        String value = prompt("New value: ");

        digitalIdService.updateAttribute(org, id, attribute, value);
        out.println();
        out.println("Updated " + attribute + " on " + id);
    }

    private void handleChangeStatus(Organisation org) {
        String id = prompt("Digital ID: ");
        if (preflightId(id, true).isEmpty()) {
            return;
        }
        String statusInput = prompt("New status (ACTIVE, SUSPENDED, REVOKED): ");

        IdStatus newStatus;
        try {
            newStatus = IdStatus.valueOf(statusInput.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            out.println("Error: unknown status '" + statusInput + "'");
            return;
        }

        String reason = null;
        if (newStatus == IdStatus.SUSPENDED || newStatus == IdStatus.REVOKED) {
            reason = prompt("Reason: ");
        }

        digitalIdService.changeStatus(org, id, newStatus, reason);
        out.println();
        out.println("Status of " + id + " is now " + newStatus);
    }

    private void handleVerify(Organisation org) {
        String id = prompt("Digital ID: ");
        VerificationResult result = digitalIdService.verify(org, id);

        out.println();
        out.println("Verification of " + result.getDigitalIdId());
        out.println("  Status: " + result.getStatus());
        if (result.getAttributes().isEmpty()) {
            out.println("  (No additional attributes accessible to " + org.getName() + ")");
        } else {
            out.println("  Attributes:");
            result.getAttributes().forEach((key, value) ->
                    out.println("    " + key + ": " + formatValueForDisplay(key, value)));
        }
    }

    private void handleViewAuditLogs(Organisation org) {
        List<AuditEntry> entries = auditService.getAllEntries(org);
        out.println();
        printAuditEntries(entries);
    }

    private void handleViewAuditLogsByDigitalId(Organisation org) {
        String id = prompt("Digital ID: ");
        List<AuditEntry> entries = auditService.getEntriesByDigitalId(org, id);
        out.println();
        printAuditEntries(entries);
    }

    private void handleStatusHistory(Organisation org) {
        String id = prompt("Digital ID: ");
        if (preflightId(id, false).isEmpty()) {
            return;
        }
        LocalDate start = parseUiDate(prompt("Start date (dd/MM/yyyy): "));
        LocalDate end = parseUiDate(prompt("End date (dd/MM/yyyy): "));

        List<AuditEntry> entries = auditService.getStatusHistory(org, id, start, end);
        out.println();
        if (entries.isEmpty()) {
            out.println("No status changes found for " + id +
                    " between " + start.format(UI_DATE_FORMAT) +
                    " and " + end.format(UI_DATE_FORMAT) + ".");
            return;
        }
        printAuditEntries(entries);
    }

    private void handlePeriodVerify(Organisation org) {
        String id = prompt("Digital ID: ");
        if (preflightId(id, false).isEmpty()) {
            return;
        }
        LocalDate start = parseUiDate(prompt("Start date (dd/MM/yyyy): "));
        LocalDate end = parseUiDate(prompt("End date (dd/MM/yyyy): "));

        PeriodVerificationResult result =
                digitalIdService.verifyContinuousActivity(org, id, start, end);

        out.println();
        out.println("Period verification of " + result.getDigitalIdId());
        out.println("  Period: " + result.getStartDate().format(UI_DATE_FORMAT)
                + " to " + result.getEndDate().format(UI_DATE_FORMAT));
        out.println("  Continuously active: " + result.isContinuouslyActive());
        if (!result.getStatusEventsInPeriod().isEmpty()) {
            out.println("  Status changes in period:");
            for (AuditEntry e : result.getStatusEventsInPeriod()) {
                out.println("    " + e.getTimestamp() + " | " + e.getDetails());
            }
        }
    }


    // ----- Helpers -----

    private void printAuditEntries(List<AuditEntry> entries) {
        if (entries.isEmpty()) {
            out.println("(No audit entries.)");
            return;
        }
        for (AuditEntry e : entries) {
            out.println(e.getTimestamp() + " | " + e.getEventType() +
                    " | " + e.getDigitalIdId() +
                    " | " + e.getOrganisation() +
                    " | " + e.getDetails());
        }
    }

    private String prompt(String message) {
        out.print(message);
        if (!in.hasNextLine()) {
            return "";
        }
        return in.nextLine().trim();
    }

    private String promptIsoDate(String message) {
        while (true) {
            String raw = prompt(message);
            try {
                LocalDate parsed = parseUiDate(raw);
                return parsed.toString();
            } catch (IllegalArgumentException e) {
                out.println("Invalid date. Please use dd/MM/yyyy.");
            }
        }
    }

    private LocalDate parseUiDate(String raw) {
        try {
            return LocalDate.parse(raw, UI_DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Invalid date '" + raw + "', expected dd/MM/yyyy", e);
        }
    }

    private String formatValueForDisplay(String key, String value) {
        if ("dateOfBirth".equals(key)) {
            try {
                return LocalDate.parse(value).format(UI_DATE_FORMAT);
            } catch (DateTimeParseException e) {
                return value;
            }
        }
        return value;
    }

    private Integer readNumericChoice() {
        while (true) {
            if (!in.hasNextLine()) {
                return null;
            }
            String line = in.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                out.println("Please enter a number.");
                out.print("Enter choice: ");
            }
        }
    }

    private Optional<DigitalId> preflightId(String id, boolean requireAlterable) {
        Optional<DigitalId> existing = digitalIdService.findById(id);
        if (existing.isEmpty()) {
            out.println();
            out.println("Error: Digital ID not found: " + id);
            return Optional.empty();
        }
        if (requireAlterable && existing.get().getStatus() == IdStatus.REVOKED) {
            out.println();
            out.println("Error: Cannot perform this operation on a REVOKED Digital ID");
            return Optional.empty();
        }
        return existing;
    }

}
