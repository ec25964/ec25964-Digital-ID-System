package com.github.ec25964.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvParserWriterTest {

    private final CsvParser parser = new CsvParser();
    private final CsvWriter writer = new CsvWriter();

    @TempDir
    Path tempDir;

    @Test
    void parseMissingFileReturnsEmptyList() {
        Path missing = tempDir.resolve("nonexistent.csv");
        List<String[]> rows = parser.parse(missing);
        assertTrue(rows.isEmpty());
    }

    @Test
    void writeAndParseRoundTrip() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"name", "age", "city"};
        List<String[]> data = List.of(
                new String[]{"John", "30", "London"},
                new String[]{"Jane", "25", "Paris"}
        );

        writer.write(file, data, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(2, parsed.size());
        assertArrayEquals(new String[]{"John", "30", "London"}, parsed.get(0));
        assertArrayEquals(new String[]{"Jane", "25", "Paris"}, parsed.get(1));
    }

    @Test
    void appendRowCreatesFileWithHeader() {
        Path file = tempDir.resolve("audit.csv");
        String[] header = {"event", "id", "timestamp"};
        String[] row = {"CREATION", "abc-123", "2024-01-15T10:00:00"};

        writer.appendRow(file, row, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(row, parsed.get(0));
    }

    @Test
    void appendRowAddsToExistingFile() {
        Path file = tempDir.resolve("audit.csv");
        String[] header = {"event", "id"};

        writer.appendRow(file, new String[]{"CREATION", "id-1"}, header);
        writer.appendRow(file, new String[]{"UPDATE", "id-2"}, header);

        List<String[]> parsed = parser.parse(file);

        assertEquals(2, parsed.size());
        assertArrayEquals(new String[]{"CREATION", "id-1"}, parsed.get(0));
        assertArrayEquals(new String[]{"UPDATE", "id-2"}, parsed.get(1));
    }

    @Test
    void writeOverwritesExistingFile() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"name"};

        List<String[]> oldData = new ArrayList<>();
        oldData.add(new String[]{"old"});
        writer.write(file, oldData, header);

        List<String[]> newData = new ArrayList<>();
        newData.add(new String[]{"new"});
        writer.write(file, newData, header);


        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(new String[]{"new"}, parsed.get(0));
    }

    @Test
    void trailingEmptyFieldsPreserved() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"a", "b", "c"};
        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"x", "", ""});


        writer.write(file, data, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(new String[]{"x", "", ""}, parsed.get(0));
    }

    @Test
    void fieldContainingCommaIsRoundTripped() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"name", "address"};
        List<String[]> data = List.<String[]>of(
            new String[]{"Chaewon Kim", "Hybe Office, Seoul"}
    );


        writer.write(file, data, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(new String[]{"Chaewon Kim", "Hybe Office, Seoul"},
                parsed.get(0));
    }

    @Test
    void fieldContainingDoubleQuoteIsRoundTripped() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"alias"};
        List<String[]> data = List.<String[]>of(
            new String[]{"They call me \"the boss\""}
        );
        writer.write(file, data, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(
                new String[]{"They call me \"the boss\""},
                parsed.get(0));
    }

    @Test
    void fieldContainingBothCommaAndQuoteIsRoundTripped() {
        Path file = tempDir.resolve("test.csv");
        String[] header = {"note"};
        List<String[]> data = List.<String[]>of(
            new String[]{"He said \"hello, world\" loudly"}
        );

        writer.write(file, data, header);
        List<String[]> parsed = parser.parse(file);

        assertEquals(1, parsed.size());
        assertArrayEquals(
                new String[]{"He said \"hello, world\" loudly"},
                parsed.get(0));
    }

    @Test
    void appendedRowWithCommaSurvivesRoundTrip() {
        Path file = tempDir.resolve("audit.csv");
        String[] header = {"event", "details"};

        writer.appendRow(file,
                new String[]{"STATUS_CHANGE",
                        "ACTIVE -> SUSPENDED | Reason: Fraud, witness pending"},
                header);

        List<String[]> parsed = parser.parse(file);
        assertEquals(1, parsed.size());
        assertEquals(
                "ACTIVE -> SUSPENDED | Reason: Fraud, witness pending",
                parsed.get(0)[1]);
    }

}
