package com.attendance.ui;

import com.attendance.model.AttendanceRecord;
import com.attendance.model.AttendanceStatus;
import com.attendance.model.ClassSession;
import com.attendance.model.Gender;
import com.attendance.model.Student;
import com.attendance.service.AttendanceManager;
import com.attendance.service.AttendanceSummary;
import com.attendance.service.ReportWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

public class AttendanceFrame extends JFrame {
    private static final Color NAVY = new Color(24, 48, 76);
    private static final Color TEAL = new Color(24, 145, 142);
    private static final Color PAGE = new Color(246, 248, 251);
    private static final Color BORDER = new Color(218, 225, 233);
    private static final Color MUTED = new Color(92, 107, 122);
    private static final Color INK = new Color(35, 46, 58);
    private static final Color SURFACE = Color.WHITE;

    private final AttendanceManager manager;
    private final ReportWriter reportWriter;
    private final Path reportsDirectory;

    private final DefaultTableModel studentTableModel = new DefaultTableModel(
            new Object[]{"Roll No", "Name", "Gender", "Class/Course", "Section"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable studentTable = new JTable(studentTableModel);
    private final JTextField studentSearchField = new JTextField(22);
    private final JTextField rollField = new JTextField(14);
    private final JTextField nameField = new JTextField(18);
    private final JComboBox<Gender> genderCombo = new JComboBox<>(Gender.values());
    private final JTextField courseField = new JTextField(16);
    private final JTextField sectionField = new JTextField(8);

    private final JTextField dateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField classField = new JTextField("Java Programming", 18);
    private final JTextField timeField = new JTextField("09:30 AM", 10);
    private final DefaultTableModel attendanceTableModel = new DefaultTableModel(
            new Object[]{"Roll No", "Name", "Gender", "Status", "Note"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3 || column == 4;
        }
    };
    private final JTable attendanceTable = new JTable(attendanceTableModel);
    private final JLabel statusLabel = new JLabel("Ready");

    private final JTextField reportDateField = new JTextField(LocalDate.now().toString(), 10);
    private final JTextField reportClassField = new JTextField("Java Programming", 18);
    private final JTextField reportTimeField = new JTextField("09:30 AM", 10);
    private final JTextArea reportArea = new JTextArea();

    public AttendanceFrame(AttendanceManager manager, Path reportsDirectory) {
        super("Attendance Management System");
        this.manager = manager;
        this.reportWriter = new ReportWriter(manager);
        this.reportsDirectory = reportsDirectory;

        configureLookAndFeel();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 700));
        setSize(1180, 760);
        setLocationByPlatform(true);
        getContentPane().setBackground(PAGE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        tabs.setForeground(NAVY);
        tabs.setBackground(SURFACE);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 14, 0, 14));
        tabs.addTab("Students", createStudentsPanel());
        tabs.addTab("Take Attendance", createAttendancePanel());
        tabs.addTab("Reports", createReportsPanel());

        add(createHeader(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        add(statusLabel, BorderLayout.SOUTH);

        refreshStudents();
        loadAttendanceTable(false);
        showTodayReport();
    }

    private JPanel createStudentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 22, 18, 22));
        panel.setBackground(PAGE);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        top.setOpaque(false);
        JLabel searchLabel = new JLabel("Find a student");
        searchLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        searchLabel.setForeground(INK);
        top.add(searchLabel);
        top.add(studentSearchField);
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(event -> refreshStudents());
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(event -> {
            studentSearchField.setText("");
            refreshStudents();
        });
        styleButton(searchButton, TEAL);
        styleButton(clearButton, NAVY);
        top.add(searchButton);
        top.add(clearButton);
        panel.add(top, BorderLayout.NORTH);

        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(24);
        styleTable(studentTable);
        studentTable.getSelectionModel().addListSelectionListener(event -> fillStudentFormFromSelection());
        JScrollPane studentScrollPane = new JScrollPane(studentTable);
        styleScrollPane(studentScrollPane);
        panel.add(studentScrollPane, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(sectionBorder("Student details"));
        form.setBackground(SURFACE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addField(form, gbc, 0, "Roll No", rollField);
        addField(form, gbc, 1, "Name", nameField);
        addField(form, gbc, 2, "Gender", genderCombo);
        addField(form, gbc, 3, "Class/Course", courseField);
        addField(form, gbc, 4, "Section", sectionField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.setOpaque(false);
        JButton addButton = new JButton("Add");
        addButton.addActionListener(event -> addStudent());
        JButton updateButton = new JButton("Update");
        updateButton.addActionListener(event -> updateStudent());
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(event -> deleteStudent());
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(event -> clearStudentForm());
        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deleteButton);
        buttons.add(resetButton);
        styleButton(addButton, TEAL);
        styleButton(updateButton, NAVY);
        styleButton(deleteButton, new Color(177, 70, 66));
        styleButton(resetButton, MUTED);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        form.add(buttons, gbc);

        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 22, 18, 22));
        panel.setBackground(PAGE);

        JPanel session = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        session.setOpaque(false);
        session.add(new JLabel("Date"));
        session.add(dateField);
        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(event -> {
            dateField.setText(LocalDate.now().toString());
            loadAttendanceTable(true);
        });
        session.add(todayButton);
        session.add(new JLabel("Class"));
        session.add(classField);
        session.add(new JLabel("Time"));
        session.add(timeField);

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(event -> loadAttendanceTable(true));
        JButton saveButton = new JButton("Save Attendance");
        saveButton.addActionListener(event -> saveAttendanceFromTable());
        JButton reportButton = new JButton("Show Report");
        reportButton.addActionListener(event -> showAttendanceSessionReport());
        session.add(loadButton);
        session.add(saveButton);
        session.add(reportButton);
        styleButton(todayButton, NAVY);
        styleButton(loadButton, TEAL);
        styleButton(saveButton, new Color(37, 119, 76));
        styleButton(reportButton, NAVY);
        panel.add(session, BorderLayout.NORTH);

        attendanceTable.setRowHeight(24);
        styleTable(attendanceTable);
        TableColumn statusColumn = attendanceTable.getColumnModel().getColumn(3);
        statusColumn.setCellEditor(new javax.swing.DefaultCellEditor(new JComboBox<>(AttendanceStatus.values())));
        JScrollPane attendanceScrollPane = new JScrollPane(attendanceTable);
        styleScrollPane(attendanceScrollPane);
        panel.add(attendanceScrollPane, BorderLayout.CENTER);

        JTextArea hint = new JTextArea("Select or type status in the Status column. Saving the same roll number, class, date, and time updates that record permanently.");
        hint.setEditable(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setBackground(new Color(235, 246, 245));
        hint.setForeground(MUTED);
        hint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 224, 221)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 22, 18, 22));
        panel.setBackground(PAGE);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        controls.setOpaque(false);
        controls.add(new JLabel("Date"));
        controls.add(reportDateField);
        controls.add(new JLabel("Class"));
        controls.add(reportClassField);
        controls.add(new JLabel("Time"));
        controls.add(reportTimeField);

        JButton todayButton = new JButton("Today");
        todayButton.addActionListener(event -> {
            reportDateField.setText(LocalDate.now().toString());
            showTodayReport();
        });
        JButton sessionButton = new JButton("Session Report");
        sessionButton.addActionListener(event -> showReportSession());
        JButton dayButton = new JButton("Day Report");
        dayButton.addActionListener(event -> showDayReport());
        JButton exportButton = new JButton("Export Session");
        exportButton.addActionListener(event -> exportSessionReport());
        controls.add(todayButton);
        controls.add(sessionButton);
        controls.add(dayButton);
        controls.add(exportButton);
        styleButton(todayButton, NAVY);
        styleButton(sessionButton, TEAL);
        styleButton(dayButton, NAVY);
        styleButton(exportButton, new Color(37, 119, 76));
        panel.add(controls, BorderLayout.NORTH);

        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        reportArea.setLineWrap(false);
        reportArea.setBackground(new Color(253, 254, 255));
        reportArea.setForeground(new Color(35, 46, 58));
        reportArea.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane reportScrollPane = new JScrollPane(reportArea);
        styleScrollPane(reportScrollPane);
        panel.add(reportScrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));

        JLabel title = new JLabel("ATTENDANCE HUB");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Student records   |   Daily presence   |   Reports");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(190, 211, 226));

        JPanel copy = new JPanel(new GridLayout(2, 1, 0, 2));
        copy.setOpaque(false);
        copy.add(title);
        copy.add(subtitle);
        header.add(copy, BorderLayout.WEST);

        JLabel date = new JLabel("TODAY  " + LocalDate.now(), SwingConstants.RIGHT);
        date.setFont(new Font("SansSerif", Font.BOLD, 13));
        date.setForeground(new Color(170, 224, 218));
        header.add(date, BorderLayout.EAST);
        return header;
    }

    private void configureLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the default Swing look and feel when the theme is unavailable.
        }
        UIManager.put("Panel.background", PAGE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 12));
        UIManager.put("TabbedPane.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("OptionPane.messageFont", new Font("SansSerif", Font.PLAIN, 13));
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker()),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
    }

    private void styleTable(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(SURFACE);
        table.setForeground(INK);
        table.setSelectionBackground(new Color(218, 240, 239));
        table.setSelectionForeground(new Color(25, 53, 73));
        JTableHeader header = table.getTableHeader();
        header.setBackground(NAVY);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
        scrollPane.getViewport().setBackground(SURFACE);
    }

    private javax.swing.border.Border sectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), title),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    private void addField(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        JLabel jLabel = new JLabel(label, SwingConstants.RIGHT);
        form.add(jLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(field, gbc);
    }

    private void refreshStudents() {
        studentTableModel.setRowCount(0);
        List<Student> students = manager.searchStudents(studentSearchField.getText());
        for (Student student : students) {
            studentTableModel.addRow(new Object[]{
                    student.getRollNumber(),
                    student.getName(),
                    student.getGender().getLabel(),
                    student.getCourse(),
                    student.getSection()
            });
        }
        setStatus("Showing " + students.size() + " student(s) roll-number wise.");
    }

    private void fillStudentFormFromSelection() {
        int selected = studentTable.getSelectedRow();
        if (selected < 0) {
            return;
        }
        rollField.setText(valueAt(studentTableModel, selected, 0));
        nameField.setText(valueAt(studentTableModel, selected, 1));
        genderCombo.setSelectedItem(Gender.fromInput(valueAt(studentTableModel, selected, 2)));
        courseField.setText(valueAt(studentTableModel, selected, 3));
        sectionField.setText(valueAt(studentTableModel, selected, 4));
    }

    private void addStudent() {
        try {
            manager.addStudent(rollField.getText(), nameField.getText(),
                    (Gender) genderCombo.getSelectedItem(), courseField.getText(), sectionField.getText());
            clearStudentForm();
            refreshStudents();
            loadAttendanceTable(false);
            setStatus("Student added.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void updateStudent() {
        try {
            manager.updateStudent(rollField.getText(), nameField.getText(),
                    (Gender) genderCombo.getSelectedItem(), courseField.getText(), sectionField.getText());
            refreshStudents();
            loadAttendanceTable(false);
            setStatus("Student updated.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteStudent() {
        try {
            String rollNumber = rollField.getText().trim();
            if (rollNumber.isEmpty()) {
                throw new IllegalArgumentException("Select or enter a roll number to delete.");
            }
            int answer = JOptionPane.showConfirmDialog(this,
                    "Delete student " + rollNumber + " and all attendance records?",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            manager.removeStudent(rollNumber);
            clearStudentForm();
            refreshStudents();
            loadAttendanceTable(false);
            setStatus("Student deleted.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void clearStudentForm() {
        rollField.setText("");
        nameField.setText("");
        genderCombo.setSelectedItem(Gender.MALE);
        courseField.setText("");
        sectionField.setText("");
        studentTable.clearSelection();
    }

    private void loadAttendanceTable(boolean showMessage) {
        try {
            ClassSession session = currentAttendanceSession();
            Map<String, AttendanceRecord> existing = new LinkedHashMap<>();
            for (AttendanceRecord record : manager.attendanceForSession(
                    session.getDate(), session.getClassName(), session.getClassTime())) {
                existing.put(record.getStudentId().toLowerCase(), record);
            }

            attendanceTableModel.setRowCount(0);
            for (Student student : manager.listStudents()) {
                AttendanceRecord record = existing.get(student.getRollNumber().toLowerCase());
                attendanceTableModel.addRow(new Object[]{
                        student.getRollNumber(),
                        student.getName(),
                        student.getGender().getLabel(),
                        record == null ? AttendanceStatus.PRESENT : record.getStatus(),
                        record == null ? "" : record.getNote()
                });
            }
            if (showMessage) {
                setStatus("Loaded attendance for " + session.displayName() + ".");
            }
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void saveAttendanceFromTable() {
        try {
            if (attendanceTable.isEditing()) {
                attendanceTable.getCellEditor().stopCellEditing();
            }
            ClassSession session = currentAttendanceSession();
            for (int row = 0; row < attendanceTableModel.getRowCount(); row++) {
                String rollNumber = valueAt(attendanceTableModel, row, 0);
                AttendanceStatus status = statusFromCell(attendanceTableModel.getValueAt(row, 3));
                String note = valueAt(attendanceTableModel, row, 4);
                manager.markAttendance(session.getDate(), session.getClassName(), session.getClassTime(),
                        rollNumber, status, note);
            }
            setStatus("Attendance saved permanently for " + session.displayName() + ".");
            showAttendanceSessionReport();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showAttendanceSessionReport() {
        try {
            ClassSession session = currentAttendanceSession();
            reportDateField.setText(session.getDate().toString());
            reportClassField.setText(session.getClassName());
            reportTimeField.setText(session.getClassTime());
            reportArea.setText(buildSessionReportText(session));
            setStatus("Report shown for " + session.displayName() + ".");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showTodayReport() {
        reportDateField.setText(LocalDate.now().toString());
        showDayReport();
    }

    private void showDayReport() {
        try {
            LocalDate date = parseDate(reportDateField.getText());
            StringBuilder text = new StringBuilder();
            List<ClassSession> sessions = manager.sessionsForDate(date);
            if (sessions.isEmpty()) {
                text.append("No attendance recorded for ").append(date).append(".");
            } else {
                for (ClassSession session : sessions) {
                    text.append(buildSessionReportText(session)).append("\n\n");
                }
            }
            reportArea.setText(text.toString());
            setStatus("Day report shown.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showReportSession() {
        try {
            ClassSession session = currentReportSession();
            reportArea.setText(buildSessionReportText(session));
            setStatus("Session report shown.");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void exportSessionReport() {
        try {
            ClassSession session = currentReportSession();
            Path file = reportWriter.writeSessionReport(session, reportsDirectory);
            setStatus("Report exported to " + file.toAbsolutePath());
            JOptionPane.showMessageDialog(this, "Report exported:\n" + file.toAbsolutePath());
        } catch (IOException | IllegalArgumentException ex) {
            showError(ex);
        }
    }

    private String buildSessionReportText(ClassSession session) {
        List<AttendanceRecord> records = manager.attendanceForSession(
                session.getDate(), session.getClassName(), session.getClassTime());
        AttendanceSummary summary = manager.summarizeSession(
                session.getDate(), session.getClassName(), session.getClassTime());

        StringBuilder text = new StringBuilder();
        text.append("Session: ").append(session.displayName()).append("\n");
        text.append("Marked: ").append(summary.getTotalMarked())
                .append(" | Present: ").append(summary.getPresent())
                .append(" | Absent: ").append(summary.getAbsent())
                .append(" | Late: ").append(summary.getLate())
                .append(" | Excused: ").append(summary.getExcused()).append("\n");
        text.append(String.format("Attendance: %.2f%%%n%n", summary.getAttendancePercentage()));
        text.append(genderBreakdown(records)).append("\n");
        text.append(statusList("Present students", records, AttendanceStatus.PRESENT)).append("\n");
        text.append(statusList("Absent students", records, AttendanceStatus.ABSENT)).append("\n");
        text.append(statusList("Late students", records, AttendanceStatus.LATE)).append("\n");
        text.append(statusList("Excused students", records, AttendanceStatus.EXCUSED)).append("\n");
        text.append(unmarkedList(records));
        return text.toString();
    }

    private String genderBreakdown(List<AttendanceRecord> records) {
        StringBuilder text = new StringBuilder();
        text.append("Gender-wise count\n");
        text.append(String.format("%-8s %-8s %-8s %-8s %-8s%n", "Gender", "Present", "Absent", "Late", "Excused"));
        text.append("--------------------------------------------\n");
        for (Gender gender : Gender.values()) {
            text.append(String.format("%-8s %-8d %-8d %-8d %-8d%n",
                    gender.getLabel(),
                    countByGenderAndStatus(records, gender, AttendanceStatus.PRESENT),
                    countByGenderAndStatus(records, gender, AttendanceStatus.ABSENT),
                    countByGenderAndStatus(records, gender, AttendanceStatus.LATE),
                    countByGenderAndStatus(records, gender, AttendanceStatus.EXCUSED)));
        }
        return text.toString();
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

    private String statusList(String label, List<AttendanceRecord> records, AttendanceStatus status) {
        StringBuilder text = new StringBuilder(label).append("\n");
        boolean found = false;
        for (AttendanceRecord record : records) {
            if (record.getStatus() == status) {
                Student student = manager.findStudent(record.getStudentId()).orElse(null);
                String name = student == null ? "Unknown" : student.getName();
                String gender = student == null ? "Unknown" : student.getGender().getLabel();
                text.append(record.getStudentId()).append(" - ").append(name)
                        .append(" (").append(gender).append(")\n");
                found = true;
            }
        }
        if (!found) {
            text.append("None\n");
        }
        return text.toString();
    }

    private String unmarkedList(List<AttendanceRecord> records) {
        StringBuilder text = new StringBuilder("Not marked yet\n");
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
                text.append(student.getRollNumber()).append(" - ").append(student.getName())
                        .append(" (").append(student.getGender().getLabel()).append(")\n");
                found = true;
            }
        }
        if (!found) {
            text.append("None\n");
        }
        return text.toString();
    }

    private ClassSession currentAttendanceSession() {
        return new ClassSession(parseDate(dateField.getText()), classField.getText(), timeField.getText());
    }

    private ClassSession currentReportSession() {
        return new ClassSession(parseDate(reportDateField.getText()), reportClassField.getText(), reportTimeField.getText());
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter date as YYYY-MM-DD.");
        }
    }

    private AttendanceStatus statusFromCell(Object value) {
        if (value instanceof AttendanceStatus) {
            return (AttendanceStatus) value;
        }
        return AttendanceStatus.fromInput(String.valueOf(value));
    }

    private String valueAt(DefaultTableModel model, int row, int column) {
        Object value = model.getValueAt(row, column);
        return value == null ? "" : String.valueOf(value);
    }

    private void setStatus(String message) {
        statusLabel.setText("  " + message);
    }

    private void showError(Exception ex) {
        setStatus("Error: " + ex.getMessage());
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
