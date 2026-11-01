package com.attendance.ui;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.ClassSession;
import com.attendance.model.Gender;
import com.attendance.model.Student;
import com.attendance.service.AttendanceManager;
import com.attendance.service.AttendanceSummary;
import com.attendance.service.ReportWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class ConsoleUi {
    private static final int PAGE_SIZE = 12;

    private final Scanner scanner;
    private final AttendanceManager manager;
    private final ReportWriter reportWriter;
    private final Path reportsDirectory;

    public ConsoleUi(Scanner scanner, AttendanceManager manager, Path reportsDirectory) {
        this.scanner = scanner;
        this.manager = manager;
        this.reportWriter = new ReportWriter(manager);
        this.reportsDirectory = reportsDirectory;
    }

    public void start() {
        printStartup();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Choose an option");
            try {
                switch (choice) {
                    case "1":
                        addStudent();
                        break;
                    case "2":
                        updateStudent();
                        break;
                    case "3":
                        removeStudent();
                        break;
                    case "4":
                        listAndSearchStudents();
                        break;
                    case "5":
                        takeAttendance();
                        break;
                    case "6":
                        editTodayAttendance();
                        break;
                    case "7":
                        viewAttendanceData();
                        break;
                    case "8":
                        viewStudentReport();
                        break;
                    case "9":
                        exportReports();
                        break;
                    case "10":
                        running = false;
                        break;
                    default:
                        System.out.println("Please choose a number from 1 to 10.");
                        break;
                }
            } catch (IllegalArgumentException | IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    private void printStartup() {
        System.out.println("Attendance Management System");
        System.out.println("Students: " + manager.getStudentCount()
                + " | Attendance records: " + manager.getAttendanceRecordCount());
        System.out.println("Data is permanently saved in the device data folder using SQLite.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. Add student");
        System.out.println("2. Edit student");
        System.out.println("3. Remove student");
        System.out.println("4. List or search students roll-number wise");
        System.out.println("5. Take attendance for class/date/time");
        System.out.println("6. Edit today's attendance");
        System.out.println("7. Show attendance data");
        System.out.println("8. Show one student attendance report");
        System.out.println("9. Export reports");
        System.out.println("10. Exit");
    }

    private void addStudent() throws IOException {
        String rollNumber = prompt("Roll number");
        String name = prompt("Name");
        Gender gender = promptGender("Gender (M=Boy, F=Girl, O=Other)");
        String course = prompt("Class/Course");
        String section = prompt("Section");
        Student student = manager.addStudent(rollNumber, name, gender, course, section);
        System.out.println("Added: " + student);
    }

    private void updateStudent() throws IOException {
        String rollNumber = prompt("Roll number to edit");
        Student existing = manager.findStudent(rollNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + rollNumber));
        System.out.println("Current: " + existing);
        String name = promptWithDefault("Name", existing.getName());
        Gender gender = promptGenderWithDefault("Gender (M/F/O)", existing.getGender());
        String course = promptWithDefault("Class/Course", existing.getCourse());
        String section = promptWithDefault("Section", existing.getSection());
        Student updated = manager.updateStudent(rollNumber, name, gender, course, section);
        System.out.println("Updated: " + updated);
    }

    private void removeStudent() throws IOException {
        String rollNumber = prompt("Roll number to remove");
        String confirm = prompt("Type DELETE to remove the student and their attendance records");
        if (!"DELETE".equals(confirm)) {
            System.out.println("Removal cancelled.");
            return;
        }
        manager.removeStudent(rollNumber);
        System.out.println("Student removed.");
    }

    private void listAndSearchStudents() {
        String query = promptOptional("Search by roll number, name, gender, class, or section");
        List<Student> students = manager.searchStudents(query);
        if (students.isEmpty()) {
            System.out.println("No matching students found.");
            return;
        }
        printStudentsPaged(students);
    }

    private void takeAttendance() throws IOException {
        if (manager.getStudentCount() == 0) {
            System.out.println("Add students first.");
            return;
        }

        ClassSession session = promptSession(false);
        String all = prompt("Take attendance for all students roll-number wise? (y/n)");
        if (all.equalsIgnoreCase("y")) {
            takeAttendanceForAll(session);
        } else {
            takeAttendanceBySearch(session);
        }
        printSessionReport(session);
    }

    private void takeAttendanceForAll(ClassSession session) throws IOException {
        System.out.println("Press Enter to keep the shown status, or enter P/A/L/E.");
        for (Student student : manager.listStudents()) {
            AttendanceRecord existing = findRecord(session, student.getRollNumber());
            String current = existing == null ? "not marked" : existing.getStatus().name();
            System.out.println();
            System.out.println(student.getRollNumber() + " - " + student.getName()
                    + " (" + student.getGender().getLabel() + "), current: " + current);
            AttendanceStatus status = promptStatusAllowDefault(existing == null ? null : existing.getStatus());
            String note = promptWithDefault("Note", existing == null ? "" : existing.getNote());
            manager.markAttendance(session.getDate(), session.getClassName(), session.getClassTime(),
                    student.getRollNumber(), status, note);
        }
    }

    private void takeAttendanceBySearch(ClassSession session) throws IOException {
        boolean taking = true;
        while (taking) {
            System.out.println();
            System.out.println("Type roll number to mark, LIST to browse, SEARCH to find, or DONE to finish.");
            String action = prompt("Attendance command");
            if (action.equalsIgnoreCase("DONE")) {
                taking = false;
            } else if (action.equalsIgnoreCase("LIST")) {
                printStudentsPaged(manager.listStudents());
            } else if (action.equalsIgnoreCase("SEARCH")) {
                String query = prompt("Search text");
                printStudentsPaged(manager.searchStudents(query));
            } else {
                markOneStudent(session, action);
            }
        }
    }

    private void markOneStudent(ClassSession session, String rollNumber) throws IOException {
        Student student = manager.findStudent(rollNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + rollNumber));
        AttendanceRecord existing = findRecord(session, student.getRollNumber());
        if (existing != null) {
            System.out.println("Existing: " + existing.getStatus() + " | Note: " + existing.getNote());
        }
        AttendanceStatus status = promptStatusAllowDefault(existing == null ? null : existing.getStatus());
        String note = promptWithDefault("Note", existing == null ? "" : existing.getNote());
        AttendanceRecord record = manager.markAttendance(session.getDate(), session.getClassName(),
                session.getClassTime(), student.getRollNumber(), status, note);
        System.out.println("Saved: " + record.getStudentId() + " " + record.getStatus()
                + " for " + session.displayName());
    }

    private void editTodayAttendance() throws IOException {
        LocalDate today = LocalDate.now();
        List<ClassSession> sessions = manager.sessionsForDate(today);
        if (sessions.isEmpty()) {
            System.out.println("No attendance has been recorded today (" + today + ").");
            return;
        }

        ClassSession session = chooseSession(sessions);
        printSessionReport(session);
        boolean editing = true;
        while (editing) {
            String rollNumber = prompt("Roll number to edit, SEARCH, LIST, or DONE");
            if (rollNumber.equalsIgnoreCase("DONE")) {
                editing = false;
            } else if (rollNumber.equalsIgnoreCase("SEARCH")) {
                printStudentsPaged(manager.searchStudents(prompt("Search text")));
            } else if (rollNumber.equalsIgnoreCase("LIST")) {
                printSessionRecords(manager.attendanceForSession(
                        session.getDate(), session.getClassName(), session.getClassTime()));
            } else {
                markOneStudent(session, rollNumber);
            }
        }
    }

    private void viewAttendanceData() {
        System.out.println("1. Today's attendance");
        System.out.println("2. Attendance by date");
        System.out.println("3. One class/date/time session");
        String choice = prompt("Choose report type");
        if (choice.equals("1")) {
            printDateReport(LocalDate.now());
        } else if (choice.equals("2")) {
            printDateReport(promptDate("Date (YYYY-MM-DD)"));
        } else if (choice.equals("3")) {
            ClassSession session = promptSession(true);
            printSessionReport(session);
        } else {
            System.out.println("Unknown report type.");
        }
    }

    private void printDateReport(LocalDate date) {
        List<ClassSession> sessions = manager.sessionsForDate(date);
        if (sessions.isEmpty()) {
            System.out.println("No attendance has been recorded for " + date + ".");
            return;
        }
        for (ClassSession session : sessions) {
            printSessionReport(session);
        }
    }

    private void printSessionReport(ClassSession session) {
        List<AttendanceRecord> records = manager.attendanceForSession(
                session.getDate(), session.getClassName(), session.getClassTime());
        System.out.println();
        System.out.println("Session: " + session.displayName());
        if (records.isEmpty()) {
            System.out.println("No attendance has been recorded for this session.");
            return;
        }

        printSummary(manager.summarizeSession(session.getDate(), session.getClassName(), session.getClassTime()));
        printGenderBreakdown(records);
        printStatusList("Present", records, AttendanceStatus.PRESENT);
        printStatusList("Absent", records, AttendanceStatus.ABSENT);
        printStatusList("Late", records, AttendanceStatus.LATE);
        printStatusList("Excused", records, AttendanceStatus.EXCUSED);
        printUnmarkedStudents(records);
    }

    private void printSessionRecords(List<AttendanceRecord> records) {
        if (records.isEmpty()) {
            System.out.println("No attendance records found.");
            return;
        }
        System.out.printf("%-10s %-24s %-8s %-10s %s%n", "Roll No", "Name", "Gender", "Status", "Note");
        System.out.println("--------------------------------------------------------------------------");
        for (AttendanceRecord record : records) {
            Student student = manager.findStudent(record.getStudentId()).orElse(null);
            String name = student == null ? "Unknown" : student.getName();
            String gender = student == null ? "Unknown" : student.getGender().getLabel();
            System.out.printf("%-10s %-24s %-8s %-10s %s%n",
                    record.getStudentId(), name, gender, record.getStatus(), record.getNote());
        }
    }

    private void viewStudentReport() {
        String rollNumber = prompt("Roll number");
        Student student = manager.findStudent(rollNumber)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + rollNumber));
        System.out.println(student);
        List<AttendanceRecord> records = manager.attendanceForStudent(rollNumber);
        if (records.isEmpty()) {
            System.out.println("No attendance has been recorded for this student.");
            return;
        }
        System.out.printf("%-12s %-10s %-18s %-10s %s%n", "Date", "Time", "Class", "Status", "Note");
        System.out.println("----------------------------------------------------------------");
        for (AttendanceRecord record : records) {
            System.out.printf("%-12s %-10s %-18s %-10s %s%n",
                    record.getDate(), record.getClassTime(), record.getClassName(),
                    record.getStatus(), record.getNote());
        }
        printSummary(manager.summarizeStudent(rollNumber));
    }

    private void exportReports() throws IOException {
        Path classReport = reportWriter.writeClassReport(reportsDirectory);
        System.out.println("Class report exported to: " + classReport.toAbsolutePath());

        String dailyAnswer = prompt("Export a daily report too? (y/n)");
        if (dailyAnswer.equalsIgnoreCase("y")) {
            LocalDate date = promptDate("Daily report date (YYYY-MM-DD)");
            Path dailyReport = reportWriter.writeDateReport(date, reportsDirectory);
            System.out.println("Daily report exported to: " + dailyReport.toAbsolutePath());
        }

        String sessionAnswer = prompt("Export one class session report too? (y/n)");
        if (sessionAnswer.equalsIgnoreCase("y")) {
            ClassSession session = promptSession(true);
            Path sessionReport = reportWriter.writeSessionReport(session, reportsDirectory);
            System.out.println("Session report exported to: " + sessionReport.toAbsolutePath());
        }
    }

    private ClassSession promptSession(boolean allowExistingPick) {
        LocalDate date = promptDateWithDefault("Class date (YYYY-MM-DD)", LocalDate.now());
        if (allowExistingPick) {
            List<ClassSession> sessions = manager.sessionsForDate(date);
            if (!sessions.isEmpty()) {
                String useExisting = prompt("Use an existing session for this date? (y/n)");
                if (useExisting.equalsIgnoreCase("y")) {
                    return chooseSession(sessions);
                }
            }
        }
        String className = prompt("Class/Subject name");
        String classTime = prompt("Class time (example 09:30 AM)");
        return new ClassSession(date, className, classTime);
    }

    private ClassSession chooseSession(List<ClassSession> sessions) {
        for (int i = 0; i < sessions.size(); i++) {
            System.out.println((i + 1) + ". " + sessions.get(i).displayName());
        }
        while (true) {
            String input = prompt("Choose session number");
            try {
                int index = Integer.parseInt(input);
                if (index >= 1 && index <= sessions.size()) {
                    return sessions.get(index - 1);
                }
            } catch (NumberFormatException ex) {
                System.out.println("Enter a valid number.");
            }
        }
    }

    private AttendanceRecord findRecord(ClassSession session, String rollNumber) {
        for (AttendanceRecord record : manager.attendanceForSession(
                session.getDate(), session.getClassName(), session.getClassTime())) {
            if (record.getStudentId().equalsIgnoreCase(rollNumber)) {
                return record;
            }
        }
        return null;
    }

    private void printStudentsPaged(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        for (int i = 0; i < students.size(); i++) {
            if (i % PAGE_SIZE == 0) {
                System.out.printf("%-10s %-24s %-8s %-18s %-10s%n",
                        "Roll No", "Name", "Gender", "Class/Course", "Section");
                System.out.println("--------------------------------------------------------------------------");
            }
            Student student = students.get(i);
            System.out.printf("%-10s %-24s %-8s %-18s %-10s%n",
                    student.getRollNumber(), student.getName(), student.getGender().getLabel(),
                    student.getCourse(), student.getSection());
            if ((i + 1) % PAGE_SIZE == 0 && i + 1 < students.size()) {
                String next = prompt("Press Enter for more, or Q to stop");
                if (next.equalsIgnoreCase("q")) {
                    return;
                }
            }
        }
    }

    private void printSummary(AttendanceSummary summary) {
        System.out.println("Marked: " + summary.getTotalMarked()
                + " | Present: " + summary.getPresent()
                + " | Absent: " + summary.getAbsent()
                + " | Late: " + summary.getLate()
                + " | Excused: " + summary.getExcused());
        System.out.printf("Attendance percentage: %.2f%%%n", summary.getAttendancePercentage());
    }

    private void printGenderBreakdown(List<AttendanceRecord> records) {
        System.out.println();
        System.out.println("Gender-wise count");
        System.out.printf("%-8s %-8s %-8s %-8s %-8s%n", "Gender", "Present", "Absent", "Late", "Excused");
        System.out.println("--------------------------------------------");
        printGenderLine(records, Gender.MALE);
        printGenderLine(records, Gender.FEMALE);
        printGenderLine(records, Gender.OTHER);
    }

    private void printGenderLine(List<AttendanceRecord> records, Gender gender) {
        System.out.printf("%-8s %-8d %-8d %-8d %-8d%n",
                gender.getLabel(),
                countByGenderAndStatus(records, gender, AttendanceStatus.PRESENT),
                countByGenderAndStatus(records, gender, AttendanceStatus.ABSENT),
                countByGenderAndStatus(records, gender, AttendanceStatus.LATE),
                countByGenderAndStatus(records, gender, AttendanceStatus.EXCUSED));
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

    private void printStatusList(String label, List<AttendanceRecord> records, AttendanceStatus status) {
        System.out.println();
        System.out.println(label + " students");
        boolean found = false;
        for (AttendanceRecord record : records) {
            if (record.getStatus() == status) {
                Student student = manager.findStudent(record.getStudentId()).orElse(null);
                String name = student == null ? "Unknown" : student.getName();
                String gender = student == null ? "Unknown" : student.getGender().getLabel();
                System.out.println(record.getStudentId() + " - " + name + " (" + gender + ")");
                found = true;
            }
        }
        if (!found) {
            System.out.println("None");
        }
    }

    private void printUnmarkedStudents(List<AttendanceRecord> records) {
        System.out.println();
        System.out.println("Not marked yet");
        boolean found = false;
        for (Student student : manager.listStudents()) {
            boolean marked = false;
            for (AttendanceRecord record : records) {
                if (record.getStudentId().equalsIgnoreCase(student.getRollNumber())) {
                    marked = true;
                    break;
                }
            }
            if (!marked) {
                System.out.println(student.getRollNumber() + " - " + student.getName()
                        + " (" + student.getGender().getLabel() + ")");
                found = true;
            }
        }
        if (!found) {
            System.out.println("None");
        }
    }

    private AttendanceStatus promptStatusAllowDefault(AttendanceStatus defaultStatus) {
        while (true) {
            String label = defaultStatus == null
                    ? "Status (P=Present, A=Absent, L=Late, E=Excused)"
                    : "Status (P/A/L/E) [" + defaultStatus + "]";
            String input = prompt(label);
            if (input.isEmpty() && defaultStatus != null) {
                return defaultStatus;
            }
            try {
                return AttendanceStatus.fromInput(input);
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private Gender promptGender(String label) {
        while (true) {
            try {
                return Gender.fromInput(prompt(label));
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private Gender promptGenderWithDefault(String label, Gender currentValue) {
        while (true) {
            System.out.print(label + " [" + currentValue.getLabel() + "]: ");
            String value = scanner.nextLine().trim();
            if (value.isEmpty()) {
                return currentValue;
            }
            try {
                return Gender.fromInput(value);
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private LocalDate promptDate(String label) {
        while (true) {
            try {
                return LocalDate.parse(prompt(label));
            } catch (DateTimeParseException ex) {
                System.out.println("Enter a valid date in YYYY-MM-DD format.");
            }
        }
    }

    private LocalDate promptDateWithDefault(String label, LocalDate defaultDate) {
        while (true) {
            String value = promptWithDefault(label, defaultDate.toString());
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException ex) {
                System.out.println("Enter a valid date in YYYY-MM-DD format.");
            }
        }
    }

    private String promptWithDefault(String label, String currentValue) {
        System.out.print(label + " [" + currentValue + "]: ");
        String value = scanner.nextLine().trim();
        return value.isEmpty() ? currentValue : value;
    }

    private String promptOptional(String label) {
        System.out.print(label + " (optional): ");
        return scanner.nextLine().trim();
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }
}
