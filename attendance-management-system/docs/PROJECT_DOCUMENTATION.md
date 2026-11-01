# Attendance Management System - Project Documentation

## 1. Project proposal

### Problem statement

Manual attendance registers are slow to update, difficult to search, and prone to calculation errors. This project provides a local desktop application that records attendance digitally and makes daily and class-session reports available immediately.

### Objectives

- Maintain a searchable student register using roll number, name, course, section, and gender.
- Record, correct, and retain attendance for a class, date, and time.
- Provide present, absent, late, excused, and gender-wise attendance summaries.
- Store data permanently on the device without requiring an internet connection.
- Export reports for records and review.

### Users

- Faculty members take or correct attendance.
- Course coordinators view class and date-based reports.
- Administrators maintain the student register.

## 2. Main modules

| Module | Responsibility |
| --- | --- |
| Student management | Add, update, search, sort, and remove students. |
| Attendance management | Mark or edit each student's status for a selected session. |
| Reporting | Display session, date, and student history reports; export text reports. |
| Database storage | Save students and attendance records in a local SQLite database. |
| User interface | Provide a Swing desktop interface and a console fallback. |

## 3. Rubric coverage

| Marking item | Evidence in this project |
| --- | --- |
| Concepts | Domain models represent students, sessions, records, statuses, and summaries. |
| Class design | Separate `model`, `service`, `storage`, and `ui` packages create clear responsibilities. |
| Object creation | `AttendanceManager` creates `Student` and `AttendanceRecord` objects during normal app use. |
| Constructor | Constructors validate and initialize `Student`, `ClassSession`, and `AttendanceRecord`. |
| Abstract class | `Person` is an abstract base class. |
| Polymorphism | A `Student` is handled through inherited `Person` behavior and overrides `getRole()` and `toDisplayText()`. |
| Inheritance | `Student extends Person`. |
| Interface | `AttendanceDataStore` defines storage operations; CSV and SQLite implementations implement it. |
| Exception | Input validation uses `IllegalArgumentException`; persistent storage uses checked `DataStoreException`. |
| Swing | `AttendanceFrame extends JFrame` supplies the desktop UI. |
| Database | `SQLiteAttendanceDataStore` uses JDBC, prepared statements, and `data/attendance.db`. |

## 4. Database design

The database uses two tables. The SQL definition is in `database/schema.sql`.

```text
students
  roll_number (primary key), name, gender, course, section

attendance
  attendance_date, class_name, class_time, student_id, status, note
  composite primary key: date + class + time + student
```

This composite key prevents a duplicate attendance entry for the same student in the same class session. The application uses prepared statements for all SQL inserts.

## 5. How to run

1. Install a Java JDK, then reopen VS Code.
2. Open the `attendance-management-system` folder in VS Code.
3. In its terminal, run `run.bat`.
4. The desktop application opens. Data is saved automatically in `data/attendance.db`.

`run-console.bat` opens the text menu version. Keep the supplied `lib/sqlite-jdbc-3.50.3.0.jar` file in the project because it is the database driver.

## 6. Demonstration flow

1. Open **Students** and add at least five students, including boys and girls.
2. Search for a student by roll number and edit one record.
3. Open **Take Attendance** and enter the date, class name, and time.
4. Mark a mix of Present, Absent, Late, and Excused, then save.
5. Change one status and save again to demonstrate editing existing attendance.
6. Open **Reports** to show totals, absent students, gender-wise counts, and the export feature.
7. Close and reopen the program to prove the SQLite data remains stored.

## 7. Testing checklist

| Test | Expected result |
| --- | --- |
| Add a student with a duplicate roll number | Validation message; no duplicate is saved. |
| Leave a required student field blank | Validation message is shown. |
| Mark the same student twice in one session | Existing record is updated, not duplicated. |
| Search by roll number or name | Matching students appear in the table. |
| Restart the application | Previously saved data is loaded from SQLite. |
| Export a report | A readable text file is created in `reports/`. |

## 8. Final presentation checklist

- State the problem, target users, and objectives first.
- Show the package structure and explain the model-service-storage-UI separation.
- Point out `Person`, `Student`, and `AttendanceDataStore` to demonstrate OOP concepts.
- Show the SQLite database file and `schema.sql` to demonstrate database usage.
- Demonstrate add student, take attendance, report, export, and restart persistence.
- Let each team member explain the module they implemented and answer one question.
