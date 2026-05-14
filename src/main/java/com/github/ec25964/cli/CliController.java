package com.github.ec25964.cli;

import com.github.ec25964.model.Organisation;
import com.github.ec25964.service.AuditService;
import com.github.ec25964.service.DigitalIdService;
import com.github.ec25964.service.OrganisationRegistry;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

public class CliController {

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
        out.println("(command " + choice + " not yet implemented)");
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
}
