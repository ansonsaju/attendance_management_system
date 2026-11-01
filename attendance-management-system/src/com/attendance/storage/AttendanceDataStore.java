package com.attendance.storage;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.Student;

import java.io.IOException;
import java.util.List;

public interface AttendanceDataStore {
    List<Student> loadStudents() throws IOException;

    void saveStudents(List<Student> students) throws IOException;

    List<AttendanceRecord> loadAttendance() throws IOException;

    void saveAttendance(List<AttendanceRecord> records) throws IOException;
}
