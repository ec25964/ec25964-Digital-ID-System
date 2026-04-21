package com.github.ec25964.persistence;

import com.github.ec25964.model.DigitalId;
import com.github.ec25964.model.IdStatus;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



public class DigitalIdRepository {

    private static final int COL_ID = 0;
    private static final int COL_FIRST_NAME = 1;
    private static final int COL_LAST_NAME = 2;
    private static final int COL_DATE_OF_BIRTH = 3;
    private static final int COL_ADDRESS = 4;
    private static final int COL_NATIONALITY = 5;
    private static final int COL_EMAIL = 6;
    private static final int COL_STATUS = 7;

    private static final String[] HEADER = {
            "id", "firstName", "lastName", "dateOfBirth", "address", "nationality", "email", "status"
    };

    private final Path filePath;
    private final CsvParser parser;
    private final CsvWriter writer;

    public DigitalIdRepository(Path filePath, CsvParser parser, CsvWriter writer) {
        this.filePath = filePath;
        this.parser = parser;
        this.writer = writer;
    }

    public List<DigitalId> loadAll() {
        List<String[]> rows = parser.parse(filePath);
        List<DigitalId> ids = new ArrayList<>();

        for (String[] row : rows) {
            ids.add(fromCsvRow(row));
        }

        return ids;
    }

    public Optional<DigitalId> findById(String id) {
        return loadAll().stream()
                .filter(d -> d.getId().equals(id))
                .findFirst();
    }

    public void save(DigitalId digitalId) {
        List<DigitalId> all = loadAll();
        boolean found = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(digitalId.getId())) {
                all.set(i, digitalId);
                found = true;
                break;
            }
        }

        if (!found) {
            all.add(digitalId);
        }

        List<String[]> rows = new ArrayList<>();
        for (DigitalId d : all) {
            rows.add(toCsvRow(d));
        }

        writer.write(filePath, rows, HEADER);
    }

    private DigitalId fromCsvRow(String[] row) {
        return new DigitalId(
                row[COL_ID],
                row[COL_FIRST_NAME],
                row[COL_LAST_NAME],
                LocalDate.parse(row[COL_DATE_OF_BIRTH]),
                row[COL_ADDRESS],
                row[COL_NATIONALITY],
                row[COL_EMAIL],
                IdStatus.valueOf(row[COL_STATUS])
        );
    }

    private String[] toCsvRow(DigitalId d) {
        return new String[]{
                d.getId(),
                d.getFirstName(),
                d.getLastName(),
                d.getDateOfBirth().toString(),
                d.getAddress(),
                d.getNationality(),
                d.getEmail(),
                d.getStatus().name()
        };
    }
}
