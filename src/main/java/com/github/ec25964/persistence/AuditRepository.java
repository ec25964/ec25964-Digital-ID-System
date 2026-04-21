package com.github.ec25964.persistence;

import com.github.ec25964.model.AuditEntry;
import com.github.ec25964.model.AuditEventType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditRepository {

    private static final int COL_EVENT_TYPE = 0;
    private static final int COL_DIGITAL_ID_ID = 1;
    private static final int COL_ORGANISATION = 2;
    private static final int COL_TIMESTAMP = 3;
    private static final int COL_DETAILS = 4;

    private static final String[] HEADER = {
            "eventType", "digitalIdId", "organisation", "timestamp", "details"
    };

    private final Path filePath;
    private final CsvParser parser;
    private final CsvWriter writer;

    public AuditRepository(Path filePath, CsvParser parser, CsvWriter writer) {
        this.filePath = filePath;
        this.parser = parser;
        this.writer = writer;
    }

    public List<AuditEntry> loadAll() {
        List<String[]> rows = parser.parse(filePath);
        List<AuditEntry> entries = new ArrayList<>();

        for (String[] row : rows) {
            entries.add(fromCsvRow(row));
        }

        return entries;
    }

    public void append(AuditEntry entry) {
        writer.appendRow(filePath, toCsvRow(entry), HEADER);
    }

    private AuditEntry fromCsvRow(String[] row) {
        return new AuditEntry(
                AuditEventType.valueOf(row[COL_EVENT_TYPE]),
                row[COL_DIGITAL_ID_ID],
                row[COL_ORGANISATION],
                LocalDateTime.parse(row[COL_TIMESTAMP]),
                row[COL_DETAILS]
        );
    }

    private String[] toCsvRow(AuditEntry entry) {
        return new String[]{
                entry.getEventType().name(),
                entry.getDigitalIdId(),
                entry.getOrganisation(),
                entry.getTimestamp().toString(),
                entry.getDetails()
        };
    }
}
