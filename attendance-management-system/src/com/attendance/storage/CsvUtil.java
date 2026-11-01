package com.attendance.storage;

import java.util.ArrayList;
import java.util.List;

final class CsvUtil {
    private CsvUtil() {
    }

    static String row(String... values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            escaped.add(escape(value));
        }
        return String.join(",", escaped);
    }

    static List<String> parse(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else if (ch == '"') {
                quoted = true;
            } else {
                current.append(ch);
            }
        }

        values.add(current.toString());
        return values;
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
