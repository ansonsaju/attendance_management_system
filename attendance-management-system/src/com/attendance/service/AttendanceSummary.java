package com.attendance.service;

public class AttendanceSummary {
    private final int totalMarked;
    private final int present;
    private final int absent;
    private final int late;
    private final int excused;

    public AttendanceSummary(int totalMarked, int present, int absent, int late, int excused) {
        this.totalMarked = totalMarked;
        this.present = present;
        this.absent = absent;
        this.late = late;
        this.excused = excused;
    }

    public int getTotalMarked() {
        return totalMarked;
    }

    public int getPresent() {
        return present;
    }

    public int getAbsent() {
        return absent;
    }

    public int getLate() {
        return late;
    }

    public int getExcused() {
        return excused;
    }

    public double getAttendancePercentage() {
        if (totalMarked == 0) {
            return 0.0;
        }
        return ((present + late + excused) * 100.0) / totalMarked;
    }
}
