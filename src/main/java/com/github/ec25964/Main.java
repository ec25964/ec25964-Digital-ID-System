package com.github.ec25964;

import com.github.ec25964.cli.CliController;
import com.github.ec25964.persistence.AuditRepository;
import com.github.ec25964.persistence.CsvParser;
import com.github.ec25964.persistence.CsvWriter;
import com.github.ec25964.persistence.DigitalIdRepository;
import com.github.ec25964.service.AuditService;
import com.github.ec25964.service.IdentityManagementService;
import com.github.ec25964.service.IdentityVerificationService;
import com.github.ec25964.service.OrganisationRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

public class Main {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path SAMPLE_DIR = Path.of("sample-data");
    private static final Path IDS_FILE = DATA_DIR.resolve("ids.csv");
    private static final Path AUDIT_FILE = DATA_DIR.resolve("audit.csv");

    public static void main(String[] args) {
        seedDataDirectoryIfEmpty();

        CsvParser parser = new CsvParser();
        CsvWriter writer = new CsvWriter();

        DigitalIdRepository idRepo = new DigitalIdRepository(IDS_FILE, parser, writer);
        AuditRepository auditRepo = new AuditRepository(AUDIT_FILE, parser, writer);

        AuditService auditService = new AuditService(auditRepo);
        IdentityManagementService managementService =
                new IdentityManagementService(idRepo, auditService);
        IdentityVerificationService verificationService =
                new IdentityVerificationService(idRepo, auditService, auditRepo);
        OrganisationRegistry registry = new OrganisationRegistry();

        try (Scanner scanner = new Scanner(System.in)) {
            CliController controller = new CliController(
                    managementService, verificationService, auditService,
                    registry, scanner, System.out);
            controller.start();
        }

    }

    private static void seedDataDirectoryIfEmpty() {
        try {
            Files.createDirectories(DATA_DIR);
            seedFileIfMissing(SAMPLE_DIR.resolve("ids.csv"), IDS_FILE);
            seedFileIfMissing(SAMPLE_DIR.resolve("audit.csv"), AUDIT_FILE);
        } catch (IOException e) {
            System.err.println("Warning: could not seed data directory: " + e.getMessage());
        }
    }

    private static void seedFileIfMissing(Path source, Path target) throws IOException {
        if (Files.exists(target) || !Files.exists(source)) {
            return;
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
