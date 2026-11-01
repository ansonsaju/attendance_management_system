package com.attendance.model;

import java.time.LocalDate;
import java.util.Objects;

public class ClassSession {
    private final LocalDate date;
    private final String className;
    private final String classTime;

    public ClassSession(LocalDate date, String className, String classTime) {
        this.date = Objects.requireNonNull(date, "date");
        this.className = requireValue(className, "class name");
        this.classTime = requireValue(classTime, "class time");
    }

    public LocalDate getDate() {
        return date;
    }

    public String getClassName() {
        return className;
    }

    public String getClassTime() {
        return classTime;
    }

    public String getKey() {
        return date + "|" + className.toLowerCase() + "|" + classTime.toLowerCase();
    }

    public String displayName() {
        return date + " | " + classTime + " | " + className;
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Class " + field + " cannot be blank.");
        }
        return value.trim();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClassSession)) {
            return false;
        }
        ClassSession session = (ClassSession) other;
        return getKey().equals(session.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
}
