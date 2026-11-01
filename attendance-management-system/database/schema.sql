CREATE TABLE students (
    roll_number TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    gender TEXT NOT NULL,
    course TEXT NOT NULL,
    section TEXT NOT NULL
);

CREATE TABLE attendance (
    attendance_date TEXT NOT NULL,
    class_name TEXT NOT NULL,
    class_time TEXT NOT NULL,
    student_id TEXT NOT NULL,
    status TEXT NOT NULL,
    note TEXT NOT NULL DEFAULT '',
    PRIMARY KEY (attendance_date, class_name, class_time, student_id)
);
