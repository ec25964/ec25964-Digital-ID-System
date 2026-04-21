package com.github.ec25964.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public List<String[]> parse(Path filePath) {
        List<String[]> rows = new ArrayList<>();

        if (!Files.exists(filePath)) {
            return rows;
        }

        try {
            List<String> lines = Files.readAllLines(filePath);

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (!line.isEmpty()) {
                    rows.add(line.split(",", -1));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file: " + filePath, e);
        }

        return rows;
    }
}
