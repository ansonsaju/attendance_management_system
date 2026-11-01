package com.attendance.model;

public enum AttendanceStatus {
    PRESENT("P"),
    ABSENT("A"),
    LATE("L"),
    EXCUSED("E");

    private final String code;

    AttendanceStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AttendanceStatus fromInput(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Attendance status is required.");
        }

        String normalized = input.trim().toUpperCase();
        for (AttendanceStatus status : values()) {
            if (status.name().equals(normalized) || status.code.equals(normalized)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Use P/PRESENT, A/ABSENT, L/LATE, or E/EXCUSED.");
    }
}
