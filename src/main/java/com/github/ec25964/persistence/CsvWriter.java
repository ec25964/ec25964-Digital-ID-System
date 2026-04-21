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
        lines.add(String.join(",", header));

        for (String[] row : rows) {
            lines.add(String.join(",", row));
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
                Files.write(filePath, List.of(String.join(",", header)));
            }

            String line = String.join(",", row);
            Files.write(filePath, List.of(line), StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to CSV file: " + filePath, e);
        }
    }
}
