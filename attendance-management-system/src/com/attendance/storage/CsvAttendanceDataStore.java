package com.attendance.storage;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.Gender;
import com.attendance.model.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CsvAttendanceDataStore implements AttendanceDataStore {
    private static final String STUDENT_HEADER = "rollNumber,name,gender,course,section";
    private static final String ATTENDANCE_HEADER = "date,className,classTime,studentId,status,note";

    private final Path studentsFile;
    private final Path attendanceFile;

    public CsvAttendanceDataStore(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        this.studentsFile = dataDirectory.resolve("students.csv");
        this.attendanceFile = dataDirectory.resolve("attendance.csv");
        ensureFile(studentsFile, STUDENT_HEADER);
        ensureFile(attendanceFile, ATTENDANCE_HEADER);
    }

    @Override
    public List<Student> loadStudents() throws IOException {
        List<Student> students = new ArrayList<>();
        List<String> lines = Files.readAllLines(studentsFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            List<String> values = CsvUtil.parse(lines.get(i));
            if (values.size() >= 5) {
                students.add(new Student(values.get(0), values.get(1), Gender.fromInput(values.get(2)),
                        values.get(3), values.get(4)));
            } else if (values.size() >= 4) {
                students.add(new Student(values.get(0), values.get(1), Gender.OTHER, values.get(2), values.get(3)));
            }
        }
        students.sort(CsvAttendanceDataStore::compareStudentsByRollNumber);
        return students;
    }

    @Override
    public void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(STUDENT_HEADER);
        students.stream()
                .sorted(CsvAttendanceDataStore::compareStudentsByRollNumber)
                .map(student -> CsvUtil.row(student.getRollNumber(), student.getName(),
                        student.getGender().name(), student.getCourse(), student.getSection()))
                .forEach(lines::add);
        Files.write(studentsFile, lines, StandardCharsets.UTF_8);
    }

    @Override
    public List<AttendanceRecord> loadAttendance() throws IOException {
        List<AttendanceRecord> records = new ArrayList<>();
        List<String> lines = Files.readAllLines(attendanceFile, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            List<String> values = CsvUtil.parse(lines.get(i));
            if (values.size() >= 6) {
                records.add(new AttendanceRecord(
                        LocalDate.parse(values.get(0)),
                        values.get(1),
                        values.get(2),
                        values.get(3),
                        AttendanceStatus.fromInput(values.get(4)),
                        values.get(5)));
            } else if (values.size() >= 4) {
                records.add(new AttendanceRecord(
                        LocalDate.parse(values.get(0)),
                        "General",
                        "00:00",
                        values.get(1),
                        AttendanceStatus.fromInput(values.get(2)),
                        values.get(3)));
            }
        }
        records.sort(Comparator
                .comparing(AttendanceRecord::getDate)
                .thenComparing(AttendanceRecord::getClassTime, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AttendanceRecord::getClassName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(AttendanceRecord::getStudentId, String.CASE_INSENSITIVE_ORDER));
        return records;
    }

    @Override
    public void saveAttendance(List<AttendanceRecord> records) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(ATTENDANCE_HEADER);
        records.stream()
                .sorted(Comparator
                        .comparing(AttendanceRecord::getDate)
                        .thenComparing(AttendanceRecord::getClassTime, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceRecord::getClassName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttendanceRecord::getStudentId, String.CASE_INSENSITIVE_ORDER))
                .map(record -> CsvUtil.row(
                        record.getDate().toString(),
                        record.getClassName(),
                        record.getClassTime(),
                        record.getStudentId(),
                        record.getStatus().name(),
                        record.getNote()))
                .forEach(lines::add);
        Files.write(attendanceFile, lines, StandardCharsets.UTF_8);
    }

    private static void ensureFile(Path file, String header) throws IOException {
        if (!Files.exists(file)) {
            List<String> lines = new ArrayList<>();
            lines.add(header);
            Files.write(file, lines, StandardCharsets.UTF_8);
        }
    }

    private static int compareStudentsByRollNumber(Student first, Student second) {
        return first.getRollNumber().compareToIgnoreCase(second.getRollNumber());
    }
}
