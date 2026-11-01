package com.attendance.storage;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.Gender;
import com.attendance.model.Student;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite-backed implementation of the storage interface. It keeps the data in
 * one local database file and uses prepared statements for safe SQL writes.
 */
public class SQLiteAttendanceDataStore implements AttendanceDataStore {
    private final String databaseUrl;

    public SQLiteAttendanceDataStore(Path dataDirectory) throws IOException {
        try {
            Files.createDirectories(dataDirectory);
            Class.forName("org.sqlite.JDBC");
            databaseUrl = "jdbc:sqlite:" + dataDirectory.resolve("attendance.db").toAbsolutePath();
            createSchema();
        } catch (ClassNotFoundException ex) {
            throw new DataStoreException("SQLite driver is missing. Keep lib/sqlite-jdbc.jar with the project.", ex);
        } catch (SQLException ex) {
            throw new DataStoreException("Could not create the attendance database.", ex);
        }
    }

    @Override
    public List<Student> loadStudents() throws IOException {
        String sql = "SELECT roll_number, name, gender, course, section FROM students ORDER BY roll_number COLLATE NOCASE";
        List<Student> students = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                students.add(new Student(result.getString("roll_number"), result.getString("name"),
                        Gender.fromInput(result.getString("gender")), result.getString("course"),
                        result.getString("section")));
            }
            return students;
        } catch (SQLException ex) {
            throw failure("load students", ex);
        }
    }

    @Override
    public void saveStudents(List<Student> students) throws IOException {
        String insert = "INSERT INTO students (roll_number, name, gender, course, section) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = connect(); Statement clear = connection.createStatement();
                PreparedStatement statement = connection.prepareStatement(insert)) {
            connection.setAutoCommit(false);
            clear.executeUpdate("DELETE FROM students");
            for (Student student : students) {
                statement.setString(1, student.getRollNumber());
                statement.setString(2, student.getName());
                statement.setString(3, student.getGender().name());
                statement.setString(4, student.getCourse());
                statement.setString(5, student.getSection());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            throw failure("save students", ex);
        }
    }

    @Override
    public List<AttendanceRecord> loadAttendance() throws IOException {
        String sql = "SELECT attendance_date, class_name, class_time, student_id, status, note "
                + "FROM attendance ORDER BY attendance_date, class_time, class_name, student_id";
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                records.add(new AttendanceRecord(LocalDate.parse(result.getString("attendance_date")),
                        result.getString("class_name"), result.getString("class_time"), result.getString("student_id"),
                        AttendanceStatus.fromInput(result.getString("status")), result.getString("note")));
            }
            return records;
        } catch (SQLException ex) {
            throw failure("load attendance", ex);
        }
    }

    @Override
    public void saveAttendance(List<AttendanceRecord> records) throws IOException {
        String insert = "INSERT INTO attendance (attendance_date, class_name, class_time, student_id, status, note) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect(); Statement clear = connection.createStatement();
                PreparedStatement statement = connection.prepareStatement(insert)) {
            connection.setAutoCommit(false);
            clear.executeUpdate("DELETE FROM attendance");
            for (AttendanceRecord record : records) {
                statement.setString(1, record.getDate().toString());
                statement.setString(2, record.getClassName());
                statement.setString(3, record.getClassTime());
                statement.setString(4, record.getStudentId());
                statement.setString(5, record.getStatus().name());
                statement.setString(6, record.getNote());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            throw failure("save attendance", ex);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(databaseUrl);
    }

    private void createSchema() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS students ("
                    + "roll_number TEXT PRIMARY KEY, name TEXT NOT NULL, gender TEXT NOT NULL, "
                    + "course TEXT NOT NULL, section TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS attendance ("
                    + "attendance_date TEXT NOT NULL, class_name TEXT NOT NULL, class_time TEXT NOT NULL, "
                    + "student_id TEXT NOT NULL, status TEXT NOT NULL, note TEXT NOT NULL DEFAULT '', "
                    + "PRIMARY KEY (attendance_date, class_name, class_time, student_id))");
        }
    }

    private DataStoreException failure(String action, SQLException cause) {
        return new DataStoreException("Unable to " + action + " in the SQLite database.", cause);
    }
}
