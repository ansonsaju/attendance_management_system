package com.attendance.model;

import java.time.LocalDate;
import java.util.Objects;

public class AttendanceRecord {
    private final LocalDate date;
    private final String className;
    private final String classTime;
    private final String studentId;
    private AttendanceStatus status;
    private String note;

    public AttendanceRecord(LocalDate date, String className, String classTime, String studentId,
            AttendanceStatus status, String note) {
        this.date = Objects.requireNonNull(date, "date");
        this.className = requireValue(className, "class name");
        this.classTime = requireValue(classTime, "class time");
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student id cannot be blank.");
        }
        this.studentId = studentId.trim();
        this.status = Objects.requireNonNull(status, "status");
        this.note = note == null ? "" : note.trim();
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

    public String getStudentId() {
        return studentId;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
    }

    public String getKey() {
        return date + "|" + className.toLowerCase() + "|" + classTime.toLowerCase() + "|" + studentId.toLowerCase();
    }

    public ClassSession getSession() {
        return new ClassSession(date, className, classTime);
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Attendance " + field + " cannot be blank.");
        }
        return value.trim();
    }
}
