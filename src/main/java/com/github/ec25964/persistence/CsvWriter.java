package com.github.ec25964.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CsvWriter {

    public void write(Path filePath, List<String[]> rows, String[] header) {
        List<String> lines = new ArrayList<>();
        lines.add(formatRow(header));
        for (String[] row : rows) {
            lines.add(formatRow(row));
        }

        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, lines);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV file: " + filePath, e);
        }
    }

    public void appendRow(Path filePath, String[] row, String[] header) {
        try {
            Files.createDirectories(filePath.getParent());

            if (!Files.exists(filePath)) {
                Files.write(filePath, List.of(formatRow(header)));
            }

            Files.write(filePath, List.of(formatRow(row)), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to CSV file: " + filePath, e);
        }
    }

    private String formatRow(String[] row) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeField(row[i]));
        }
        return sb.toString();
    }

    private String escapeField(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuoting =
                field.contains(",") || field.contains("\"") || field.contains("\n");
        if (!needsQuoting) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }
}
