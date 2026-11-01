package com.attendance.service;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.ClassSession;
import com.attendance.model.Gender;
import com.attendance.model.Student;
import com.attendance.storage.AttendanceDataStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AttendanceManager {
    private final AttendanceDataStore dataStore;
    private final Map<String, Student> students = new LinkedHashMap<>();
    private final Map<String, AttendanceRecord> attendanceRecords = new LinkedHashMap<>();

    public AttendanceManager(AttendanceDataStore dataStore) throws IOException {
        this.dataStore = dataStore;
        load();
    }

    public Student addStudent(String id, String name, Gender gender, String course, String section) throws IOException {
        String key = normalizeId(id);
        if (students.containsKey(key)) {
            throw new IllegalArgumentException("A student with id " + id + " already exists.");
        }
        Student student = new Student(id, name, gender, course, section);
        students.put(key, student);
        persistStudents();
        return student;
    }

    public Student updateStudent(String id, String name, Gender gender, String course, String section) throws IOException {
        Student student = findRequiredStudent(id);
        student.setName(name);
        student.setGender(gender);
        student.setCourse(course);
        student.setSection(section);
        persistStudents();
        return student;
    }

    public void removeStudent(String id) throws IOException {
        String key = normalizeId(id);
        Student removed = students.remove(key);
        if (removed == null) {
            throw new IllegalArgumentException("Student not found: " + id);
        }
        attendanceRecords.entrySet().removeIf(entry -> entry.getValue().getStudentId().equalsIgnoreCase(id));
        persistStudents();
        persistAttendance();
    }

    public List<Student> listStudents() {
        return students.values().stream()
                .sorted(AttendanceManager::compareStudentsByRollNumber)
                .collect(Collectors.toList());
    }

    public List<Student> searchStudents(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return listStudents().stream()
                .filter(student -> normalized.isEmpty()
                        || student.getRollNumber().toLowerCase().contains(normalized)
                        || student.getName().toLowerCase().contains(normalized)
                        || student.getGender().getLabel().toLowerCase().contains(normalized)
                        || student.getGender().name().toLowerCase().contains(normalized)
                        || student.getCourse().toLowerCase().contains(normalized)
                        || student.getSection().toLowerCase().contains(normalized))
                .collect(Collectors.toList());
    }

    public Optional<Student> findStudent(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(students.get(normalizeId(id)));
    }

    public AttendanceRecord markAttendance(LocalDate date, String className, String classTime, String studentId,
            AttendanceStatus status, String note) throws IOException {
        Student student = findRequiredStudent(studentId);
        AttendanceRecord record = new AttendanceRecord(date, className, classTime, student.getId(), status, note);
        attendanceRecords.put(record.getKey(), record);
        persistAttendance();
        return record;
    }

    public List<AttendanceRecord> attendanceForSession(LocalDate date, String className, String classTime) {
        ClassSession session = new ClassSession(date, className, classTime);
        return attendanceRecords.values().stream()
                .filter(record -> record.getSession().equals(session))
                .sorted(AttendanceManager::compareRecordsByStudentRollNumber)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecord> attendanceForDate(LocalDate date) {
        return attendanceRecords.values().stream()
                .filter(record -> record.getDate().equals(date))
                .sorted(Comparator
                        .comparing(AttendanceRecord::getClassTime, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceRecord::getClassName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceManager::studentRollNumberForRecord, AttendanceManager::compareRollNumbers))
                .collect(Collectors.toList());
    }

    public List<AttendanceRecord> attendanceForStudent(String studentId) {
        Student student = findRequiredStudent(studentId);
        return attendanceRecords.values().stream()
                .filter(record -> record.getStudentId().equalsIgnoreCase(student.getId()))
                .sorted(Comparator
                        .comparing(AttendanceRecord::getDate)
                        .thenComparing(AttendanceRecord::getClassTime, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceRecord::getClassName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public AttendanceSummary summarizeStudent(String studentId) {
        return summarize(attendanceForStudent(studentId));
    }

    public AttendanceSummary summarizeDate(LocalDate date) {
        return summarize(attendanceForDate(date));
    }

    public AttendanceSummary summarizeSession(LocalDate date, String className, String classTime) {
        return summarize(attendanceForSession(date, className, classTime));
    }

    public AttendanceSummary summarizeAll() {
        return summarize(attendanceRecords.values());
    }

    public int getStudentCount() {
        return students.size();
    }

    public int getAttendanceRecordCount() {
        return attendanceRecords.size();
    }

    public List<ClassSession> sessionsForDate(LocalDate date) {
        Set<ClassSession> sessions = new LinkedHashSet<>();
        attendanceForDate(date).forEach(record -> sessions.add(record.getSession()));
        return new ArrayList<>(sessions);
    }

    public List<ClassSession> allSessions() {
        Set<ClassSession> sessions = new LinkedHashSet<>();
        attendanceRecords.values().stream()
                .sorted(Comparator
                        .comparing(AttendanceRecord::getDate)
                        .thenComparing(AttendanceRecord::getClassTime, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceRecord::getClassName, String.CASE_INSENSITIVE_ORDER))
                .forEach(record -> sessions.add(record.getSession()));
        return new ArrayList<>(sessions);
    }

    private AttendanceSummary summarize(Collection<AttendanceRecord> records) {
        int present = 0;
        int absent = 0;
        int late = 0;
        int excused = 0;

        for (AttendanceRecord record : records) {
            switch (record.getStatus()) {
                case PRESENT:
                    present++;
                    break;
                case ABSENT:
                    absent++;
                    break;
                case LATE:
                    late++;
                    break;
                case EXCUSED:
                    excused++;
                    break;
                default:
                    break;
            }
        }

        return new AttendanceSummary(records.size(), present, absent, late, excused);
    }

    private void load() throws IOException {
        for (Student student : dataStore.loadStudents()) {
            students.put(normalizeId(student.getId()), student);
        }
        for (AttendanceRecord record : dataStore.loadAttendance()) {
            if (students.containsKey(normalizeId(record.getStudentId()))) {
                attendanceRecords.put(record.getKey(), record);
            }
        }
    }

    private Student findRequiredStudent(String id) {
        return findStudent(id).orElseThrow(() -> new IllegalArgumentException("Student not found: " + id));
    }

    private void persistStudents() throws IOException {
        dataStore.saveStudents(new ArrayList<>(students.values()));
    }

    private void persistAttendance() throws IOException {
        dataStore.saveAttendance(new ArrayList<>(attendanceRecords.values()));
    }

    private static String normalizeId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Student id cannot be blank.");
        }
        return id.trim().toLowerCase();
    }

    private static int compareStudentsByRollNumber(Student first, Student second) {
        return compareRollNumbers(first.getRollNumber(), second.getRollNumber());
    }

    private static int compareRecordsByStudentRollNumber(AttendanceRecord first, AttendanceRecord second) {
        return compareRollNumbers(first.getStudentId(), second.getStudentId());
    }

    private static String studentRollNumberForRecord(AttendanceRecord record) {
        return record.getStudentId();
    }

    private static int compareRollNumbers(String first, String second) {
        String left = first == null ? "" : first;
        String right = second == null ? "" : second;

        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftChar = left.charAt(leftIndex);
            char rightChar = right.charAt(rightIndex);
            if (Character.isDigit(leftChar) && Character.isDigit(rightChar)) {
                int leftStart = leftIndex;
                int rightStart = rightIndex;
                while (leftIndex < left.length() && Character.isDigit(left.charAt(leftIndex))) {
                    leftIndex++;
                }
                while (rightIndex < right.length() && Character.isDigit(right.charAt(rightIndex))) {
                    rightIndex++;
                }
                int numberCompare = Integer.valueOf(left.substring(leftStart, leftIndex))
                        .compareTo(Integer.valueOf(right.substring(rightStart, rightIndex)));
                if (numberCompare != 0) {
                    return numberCompare;
                }
            } else {
                int charCompare = Character.toLowerCase(leftChar) - Character.toLowerCase(rightChar);
                if (charCompare != 0) {
                    return charCompare;
                }
                leftIndex++;
                rightIndex++;
            }
        }
        return left.length() - right.length();
    }
}
