package com.attendance;

import com.attendance.service.AttendanceManager;
import com.attendance.storage.AttendanceDataStore;
import com.attendance.storage.CsvAttendanceDataStore;
import com.attendance.storage.SQLiteAttendanceDataStore;
import com.attendance.ui.AttendanceFrame;
import com.attendance.ui.ConsoleUi;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Path baseDirectory = args.length > 0 ? Paths.get(args[0]) : Paths.get(".");
        Path dataDirectory = baseDirectory.resolve("data");
        Path reportsDirectory = baseDirectory.resolve("reports");

        try {
            Path databaseFile = dataDirectory.resolve("attendance.db");
            boolean databaseAlreadyExists = Files.exists(databaseFile);
            SQLiteAttendanceDataStore sqliteDataStore = new SQLiteAttendanceDataStore(dataDirectory);
            if (!databaseAlreadyExists && Files.exists(dataDirectory.resolve("students.csv"))) {
                CsvAttendanceDataStore csvDataStore = new CsvAttendanceDataStore(dataDirectory);
                sqliteDataStore.saveStudents(csvDataStore.loadStudents());
                sqliteDataStore.saveAttendance(csvDataStore.loadAttendance());
            }
            AttendanceDataStore dataStore = sqliteDataStore;
            AttendanceManager manager = new AttendanceManager(dataStore);
            if (args.length > 1 && "--console".equalsIgnoreCase(args[1])) {
                try (Scanner scanner = new Scanner(System.in)) {
                    ConsoleUi ui = new ConsoleUi(scanner, manager, reportsDirectory);
                    ui.start();
                }
            } else if (GraphicsEnvironment.isHeadless()) {
                try (Scanner scanner = new Scanner(System.in)) {
                    ConsoleUi ui = new ConsoleUi(scanner, manager, reportsDirectory);
                    ui.start();
                }
            } else {
                SwingUtilities.invokeLater(() -> {
                    AttendanceFrame frame = new AttendanceFrame(manager, reportsDirectory);
                    frame.setVisible(true);
                });
            }
        } catch (Exception ex) {
            System.err.println("Unable to start Attendance Management System: " + ex.getMessage());
            System.exit(1);
        }
    }
}
