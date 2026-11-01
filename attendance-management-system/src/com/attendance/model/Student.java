package com.attendance.model;

import java.util.Objects;

public class Student extends Person {
    private final String rollNumber;
    private String course;
    private String section;

    public Student(String rollNumber, String name, Gender gender, String course, String section) {
        super(name, gender);
        this.rollNumber = requireValue(rollNumber, "roll number");
        setCourse(course);
        setSection(section);
    }

    public String getId() {
        return rollNumber;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = requireValue(course, "course");
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = requireValue(section, "section");
    }

    private static String requireValue(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Student " + field + " cannot be blank.");
        }
        return value.trim();
    }

    @Override
    public String getRole() {
        return "Student";
    }

    @Override
    public String toDisplayText() {
        return String.format("%s - %s (%s, %s, Section %s)",
                rollNumber, getName(), getGender().getLabel(), course, section);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Student)) {
            return false;
        }
        Student student = (Student) other;
        return rollNumber.equalsIgnoreCase(student.rollNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rollNumber.toLowerCase());
    }

    @Override
    public String toString() {
        return toDisplayText();
    }
}
