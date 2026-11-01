package com.attendance.service;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.ClassSession;
import com.attendance.model.Gender;
import com.attendance.model.Student;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportWriter {
    private final AttendanceManager manager;

    public ReportWriter(AttendanceManager manager) {
        this.manager = manager;
    }

    public Path writeClassReport(Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportPath = reportDirectory.resolve("attendance-report-" + LocalDate.now() + ".txt");
        List<String> lines = new ArrayList<>();

        AttendanceSummary overall = manager.summarizeAll();
        lines.add("Attendance Management System - Class Report");
        lines.add("Generated: " + LocalDate.now());
        lines.add("");
        lines.add("Students: " + manager.getStudentCount());
        lines.add("Attendance records: " + overall.getTotalMarked());
        lines.add(String.format("Overall attendance: %.2f%%", overall.getAttendancePercentage()));
        lines.add("");
        lines.add(String.format("%-12s %-24s %-8s %-12s %-8s %-8s %-8s %-8s %-8s %-8s",
                "Roll No", "Name", "Gender", "Course", "Marked", "Present", "Absent", "Late", "Excused", "Percent"));
        lines.add("------------------------------------------------------------------------------------------");

        for (Student student : manager.listStudents()) {
            AttendanceSummary summary = manager.summarizeStudent(student.getId());
            lines.add(String.format("%-12s %-24s %-8s %-12s %-8d %-8d %-8d %-8d %-8d %-7.2f%%",
                    student.getRollNumber(),
                    trim(student.getName(), 24),
                    student.getGender().getLabel(),
                    trim(student.getCourse(), 12),
                    summary.getTotalMarked(),
                    summary.getPresent(),
                    summary.getAbsent(),
                    summary.getLate(),
                    summary.getExcused(),
                    summary.getAttendancePercentage()));
        }

        Files.write(reportPath, lines, StandardCharsets.UTF_8);
        return reportPath;
    }

    public Path writeDateReport(LocalDate date, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportPath = reportDirectory.resolve("attendance-date-" + date + ".txt");
        List<String> lines = new ArrayList<>();
        AttendanceSummary summary = manager.summarizeDate(date);

        lines.add("Attendance Management System - Daily Report");
        lines.add("Date: " + date);
        lines.add("");
        lines.add("Marked: " + summary.getTotalMarked());
        lines.add("Present: " + summary.getPresent());
        lines.add("Absent: " + summary.getAbsent());
        lines.add("Late: " + summary.getLate());
        lines.add("Excused: " + summary.getExcused());
        lines.add(String.format("Attendance: %.2f%%", summary.getAttendancePercentage()));
        lines.add("");
        lines.add(String.format("%-10s %-18s %-10s %-24s %-8s %-10s %s",
                "Time", "Class", "Roll No", "Name", "Gender", "Status", "Note"));
        lines.add("----------------------------------------------------------------------------------------");

        for (AttendanceRecord record : manager.attendanceForDate(date)) {
            Student student = manager.findStudent(record.getStudentId()).orElse(null);
            String name = student == null ? "Unknown" : student.getName();
            String gender = student == null ? "Unknown" : student.getGender().getLabel();
            lines.add(String.format("%-10s %-18s %-10s %-24s %-8s %-10s %s",
                    record.getClassTime(),
                    trim(record.getClassName(), 18),
                    record.getStudentId(),
                    trim(name, 24),
                    gender,
                    record.getStatus(),
                    record.getNote()));
        }

        Files.write(reportPath, lines, StandardCharsets.UTF_8);
        return reportPath;
    }

    public Path writeSessionReport(ClassSession session, Path reportDirectory) throws IOException {
        Files.createDirectories(reportDirectory);
        Path reportPath = reportDirectory.resolve("attendance-session-"
                + session.getDate() + "-" + safeName(session.getClassTime()) + "-"
                + safeName(session.getClassName()) + ".txt");
        List<String> lines = new ArrayList<>();
        List<AttendanceRecord> records = manager.attendanceForSession(
                session.getDate(), session.getClassName(), session.getClassTime());
        AttendanceSummary summary = manager.summarizeSession(
                session.getDate(), session.getClassName(), session.getClassTime());

        lines.add("Attendance Management System - Class Session Report");
        lines.add("Session: " + session.displayName());
        lines.add("");
        lines.add("Marked: " + summary.getTotalMarked());
        lines.add("Present: " + summary.getPresent());
        lines.add("Absent: " + summary.getAbsent());
        lines.add("Late: " + summary.getLate());
        lines.add("Excused: " + summary.getExcused());
        lines.add(String.format("Attendance: %.2f%%", summary.getAttendancePercentage()));
        lines.add("");
        addGenderLines(lines, records);
        lines.add("");
        addStatusList(lines, "Present Students", records, AttendanceStatus.PRESENT);
        addStatusList(lines, "Absent Students", records, AttendanceStatus.ABSENT);
        addStatusList(lines, "Late Students", records, AttendanceStatus.LATE);
        addStatusList(lines, "Excused Students", records, AttendanceStatus.EXCUSED);

        Files.write(reportPath, lines, StandardCharsets.UTF_8);
        return reportPath;
    }

    private void addGenderLines(List<String> lines, List<AttendanceRecord> records) {
        lines.add("Gender-wise Attendance");
        lines.add(String.format("%-8s %-8s %-8s %-8s %-8s", "Gender", "Present", "Absent", "Late", "Excused"));
        lines.add("--------------------------------------------");
        addGenderLine(lines, records, Gender.MALE);
        addGenderLine(lines, records, Gender.FEMALE);
        addGenderLine(lines, records, Gender.OTHER);
    }

    private void addGenderLine(List<String> lines, List<AttendanceRecord> records, Gender gender) {
        lines.add(String.format("%-8s %-8d %-8d %-8d %-8d",
                gender.getLabel(),
                countByGenderAndStatus(records, gender, AttendanceStatus.PRESENT),
                countByGenderAndStatus(records, gender, AttendanceStatus.ABSENT),
                countByGenderAndStatus(records, gender, AttendanceStatus.LATE),
                countByGenderAndStatus(records, gender, AttendanceStatus.EXCUSED)));
    }

    private int countByGenderAndStatus(List<AttendanceRecord> records, Gender gender, AttendanceStatus status) {
        int count = 0;
        for (AttendanceRecord record : records) {
            Student student = manager.findStudent(record.getStudentId()).orElse(null);
            if (student != null && student.getGender() == gender && record.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private void addStatusList(List<String> lines, String heading, List<AttendanceRecord> records,
            AttendanceStatus status) {
        lines.add(heading + ":");
        boolean found = false;
        for (AttendanceRecord record : records) {
            if (record.getStatus() == status) {
                Student student = manager.findStudent(record.getStudentId()).orElse(null);
                String name = student == null ? "Unknown" : student.getName();
                String gender = student == null ? "Unknown" : student.getGender().getLabel();
                lines.add("  " + record.getStudentId() + " - " + name + " (" + gender + ")");
                found = true;
            }
        }
        if (!found) {
            lines.add("  None");
        }
        lines.add("");
    }

    private static String trim(String value, int length) {
        if (value == null || value.length() <= length) {
            return value == null ? "" : value;
        }
        return value.substring(0, length - 3) + "...";
    }

    private static String safeName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
