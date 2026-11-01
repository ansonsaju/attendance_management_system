package com.attendance.model;

public enum Gender {
    MALE("Boy"),
    FEMALE("Girl"),
    OTHER("Other");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Gender fromInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return OTHER;
        }

        String value = input.trim().toUpperCase();
        if (value.equals("M") || value.equals("MALE") || value.equals("BOY") || value.equals("BOYS")) {
            return MALE;
        }
        if (value.equals("F") || value.equals("FEMALE") || value.equals("GIRL") || value.equals("GIRLS")) {
            return FEMALE;
        }
        if (value.equals("O") || value.equals("OTHER")) {
            return OTHER;
        }

        throw new IllegalArgumentException("Use M/MALE/BOY, F/FEMALE/GIRL, or O/OTHER.");
    }
}
