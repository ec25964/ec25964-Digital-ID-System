package com.github.ec25964;

import com.github.ec25964.cli.CliController;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.CsvParser;
import com.github.ec25964.persistence.CsvWriter;
import com.github.ec25964.persistence.DigitalIdRepository;
import com.github.ec25964.service.AuditService;
import com.github.ec25964.service.DigitalIdService;
import com.github.ec25964.service.OrganisationRegistry;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    private static final Path IDS_FILE = Path.of("data", "ids.csv");
    private static final Path AUDIT_FILE = Path.of("data", "audit.csv");

    public static void main(String[] args) {
        CsvParser parser = new CsvParser();
        CsvWriter writer = new CsvWriter();

        DigitalIdRepository idRepo = new DigitalIdRepository(IDS_FILE, parser, writer);
        AuditRepository auditRepo = new AuditRepository(AUDIT_FILE, parser, writer);

        AuditService auditService = new AuditService(auditRepo);
        DigitalIdService digitalIdService = new DigitalIdService(idRepo, auditService);
        OrganisationRegistry registry = new OrganisationRegistry();

        try (Scanner scanner = new Scanner(System.in)) {
            CliController controller = new CliController(
                    digitalIdService, auditService, registry, scanner, System.out);
            controller.start();
        }
    }
}
