package com.attendance.model;

/**
 * Common base type for people represented by the application.
 * Student supplies the attendance-specific identity and academic details.
 */
public abstract class Person {
    private String name;
    private Gender gender;

    public Person(String name, Gender gender) {
        setName(name);
        setGender(gender);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Person name cannot be blank.");
        }
        this.name = name.trim();
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender == null ? Gender.OTHER : gender;
    }

    public abstract String getRole();

    public abstract String toDisplayText();
}
