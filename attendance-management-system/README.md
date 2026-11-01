# Java Attendance Management System

A Java desktop and console application for recording, updating, storing, and reporting student attendance digitally. The desktop interface uses Swing, while the console interface provides the same core operations for headless environments. It uses an embedded SQLite database for permanent local storage.

## Features

- Add, edit, list, search, and remove students.
- Store roll number, name, gender, class/course, and section.
- List students in roll-number order with paged output for easier scrolling.
- Search students by roll number, name, gender, class/course, or section.
- Record or update attendance for a specific class, date, and time.
- Take attendance roll-number wise for all students or search and mark selected students.
- Edit the current day's attendance.
- Track `PRESENT`, `ABSENT`, `LATE`, and `EXCUSED` statuses.
- View attendance by today, date, or one class session.
- Show total present/absent/late/excused counts.
- Show who is present, absent, late, excused, and not marked.
- Show boys/girls/other gender-wise present and absent counts.
- View per-student attendance history and percentage.
- Export class and daily reports to text files.
- Export detailed class-session reports.
- Professional desktop interface with Students, Take Attendance, and Reports tabs.
- Searchable and sortable student and attendance tables with clear status feedback.
- Persist data in the local `data/attendance.db` SQLite database.
- Demonstrate abstraction, inheritance, polymorphism, interfaces, exception handling, Swing, and JDBC database storage.

## Project Structure

```text
attendance-management-system/
  data/
    attendance.db (created automatically)
  database/
    schema.sql
  docs/
    PROJECT_DOCUMENTATION.md
  lib/
    sqlite-jdbc-3.50.3.0.jar
  reports/
  src/
    com/attendance/Main.java
    com/attendance/model/
    com/attendance/service/
    com/attendance/storage/
    com/attendance/ui/
```

## Requirements

- Java Development Kit 8 or newer.

Check Java with:

```bash
java -version
javac -version
```

## Compile

From this folder:

```bash
javac -d out $(find src -name "*.java")
```

On Windows PowerShell:

```powershell
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $sources
```

## Run

```bash
java -cp out com.attendance.Main .
```

The final `.` tells the application to use this folder for `data` and `reports`. By default, the app opens the desktop UI.

On Windows PowerShell, compile and open the desktop UI with:

```powershell
.\run.bat
```

To run the older terminal menu instead:

```powershell
.\run-console.bat
```

To open the project in VS Code:

```powershell
code .
```

## Data Format

The schema is available in `database/schema.sql`. The database file is created automatically at `data/attendance.db` when the application starts.

## Notes

- Recording attendance for the same student, class, date, and time updates the existing record.
- Removing a student also removes that student's attendance records.
- Reports are written to the `reports` folder.
- The console window itself can scroll, and long student lists pause every 12 rows.
- See `docs/PROJECT_DOCUMENTATION.md` for the proposal, marking-rubric mapping, test cases, and final presentation flow.
