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
                String line = lines.get(i);
                if (!line.isBlank()) {
                    rows.add(parseLine(line));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file: " + filePath, e);
        }

        return rows;
    }

    private String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else if (c == '"' && current.length() == 0) {
                    inQuotes = true;
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
