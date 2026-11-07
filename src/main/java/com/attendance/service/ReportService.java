package com.attendance.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.attendance.config.AppConfig;
import com.attendance.dto.AttendanceSummary;
import com.attendance.dto.SessionAttendanceData;
import com.attendance.entity.AttendanceAttempt;
import com.attendance.entity.AttendanceRecord;
import com.attendance.entity.AttendanceSession;
import com.attendance.entity.ClassSection;
import com.attendance.entity.Enrollment;
import com.attendance.entity.StudentProfile;
import com.attendance.entity.Subject;
import com.attendance.exception.AttendanceException;
import com.attendance.exception.ResourceNotFoundException;
import com.attendance.repository.AttendanceAttemptRepository;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.repository.AttendanceSessionRepository;
import com.attendance.repository.EnrollmentRepository;
import com.attendance.repository.StudentRepository;
import com.attendance.repository.SubjectRepository;
import com.attendance.repository.VerificationLogRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceAttemptRepository attemptRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubjectRepository subjectRepository;
    private final VerificationLogRepository verificationLogRepository;
    private final AttendanceService attendanceService;
    private final AppConfig appConfig;

    public ReportService(AttendanceRecordRepository recordRepository,
                         AttendanceSessionRepository sessionRepository,
                         AttendanceAttemptRepository attemptRepository,
                         StudentRepository studentRepository,
                         EnrollmentRepository enrollmentRepository,
                         SubjectRepository subjectRepository,
                         VerificationLogRepository verificationLogRepository,
                         AttendanceService attendanceService,
                         AppConfig appConfig) {
        this.recordRepository = recordRepository;
        this.sessionRepository = sessionRepository;
        this.attemptRepository = attemptRepository;
        this.studentRepository = studentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.subjectRepository = subjectRepository;
        this.verificationLogRepository = verificationLogRepository;
        this.attendanceService = attendanceService;
        this.appConfig = appConfig;
    }

    /**
     * Finds students whose overall attendance falls below the configured threshold (e.g. 75%).
     */
    public List<Map<String, Object>> getLowAttendanceStudents(Long sectionId, Double threshold) {
        double cutoff = threshold != null ? threshold : appConfig.getAttendance().getLowThresholdPercentage();
        List<Enrollment> enrollments = sectionId != null
                ? enrollmentRepository.findBySectionSectionId(sectionId)
                : enrollmentRepository.findAll();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Enrollment e : enrollments) {
            StudentProfile student = e.getStudent();
            List<AttendanceSummary> summaries = attendanceService.getStudentAttendanceSummary(student.getStudentId());

            int total = summaries.stream().mapToInt(AttendanceSummary::getTotalSessions).sum();
            int present = summaries.stream().mapToInt(AttendanceSummary::getPresentCount).sum();
            double pct = total > 0 ? (present * 100.0) / total : 0.0;

            if (total > 0 && pct < cutoff) {
                Map<String, Object> map = new HashMap<>();
                map.put("studentId", student.getStudentId());
                map.put("registerNumber", student.getRegisterNumber());
                map.put("fullName", student.getFullName());
                map.put("section", student.getSection());
                map.put("department", student.getDepartment().getDepartmentName());
                map.put("totalSessions", total);
                map.put("presentSessions", present);
                map.put("attendancePercentage", Math.round(pct * 10.0) / 10.0);
                map.put("threshold", cutoff);
                result.add(map);
            }
        }

        return result;
    }

    /**
     * Returns suspicious or rejected attendance attempts for review.
     */
    public List<Map<String, Object>> getSuspiciousAttempts() {
        List<AttendanceAttempt> rejectedAttempts = attemptRepository.findByVerificationStatus(AttendanceAttempt.VerificationStatus.REJECTED);
        List<Map<String, Object>> result = new ArrayList<>();

        for (AttendanceAttempt att : rejectedAttempts) {
            Map<String, Object> map = new HashMap<>();
            map.put("attemptId", att.getAttemptId());
            map.put("studentName", att.getStudent().getFullName());
            map.put("registerNumber", att.getStudent().getRegisterNumber());
            map.put("subject", att.getSession().getSubject().getSubjectName());
            map.put("sessionDate", att.getSession().getSessionDate().toString());
            map.put("scanTime", att.getScanTime().toString());
            map.put("rejectionReason", att.getRejectionReason());
            result.add(map);
        }

        return result;
    }

    /**
     * Generates an Excel spreadsheet (.xlsx) for an attendance session.
     */
    public byte[] generateExcelReport(Long sessionId) {
        SessionAttendanceData data = attendanceService.getSessionAttendance(sessionId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Attendance Report");

            // Header styling
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Present / Absent styling
            CellStyle presentStyle = workbook.createCellStyle();
            presentStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            presentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle absentStyle = workbook.createCellStyle();
            absentStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            absentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title rows
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("SMART ATTENDANCE REPORT");

            Row sessionInfoRow1 = sheet.createRow(2);
            sessionInfoRow1.createCell(0).setCellValue("Subject:");
            sessionInfoRow1.createCell(1).setCellValue(data.getSubjectName() + " (" + data.getSubjectCode() + ")");
            sessionInfoRow1.createCell(3).setCellValue("Section:");
            sessionInfoRow1.createCell(4).setCellValue(data.getSectionName());

            Row sessionInfoRow2 = sheet.createRow(3);
            sessionInfoRow2.createCell(0).setCellValue("Faculty:");
            sessionInfoRow2.createCell(1).setCellValue(data.getFacultyName());
            sessionInfoRow2.createCell(3).setCellValue("Date:");
            sessionInfoRow2.createCell(4).setCellValue(data.getSessionDate().toString());

            Row statsRow = sheet.createRow(4);
            statsRow.createCell(0).setCellValue("Present: " + data.getPresentCount());
            statsRow.createCell(1).setCellValue("Absent: " + data.getAbsentCount());
            statsRow.createCell(2).setCellValue("Total: " + data.getTotalEnrolled());

            // Table Header
            Row tableHeader = sheet.createRow(6);
            String[] columns = {"S.No", "Register Number", "Student Name", "Status", "Marked Time"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = tableHeader.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 7;
            int sNo = 1;

            // Present students
            for (SessionAttendanceData.StudentAttendanceInfo s : data.getPresentStudents()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sNo++);
                row.createCell(1).setCellValue(s.getRegisterNumber());
                row.createCell(2).setCellValue(s.getStudentName());
                Cell statusCell = row.createCell(3);
                statusCell.setCellValue("PRESENT");
                statusCell.setCellStyle(presentStyle);
                row.createCell(4).setCellValue(s.getMarkedAt());
            }

            // Absent students
            for (SessionAttendanceData.StudentAttendanceInfo s : data.getAbsentStudents()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(sNo++);
                row.createCell(1).setCellValue(s.getRegisterNumber());
                row.createCell(2).setCellValue(s.getStudentName());
                Cell statusCell = row.createCell(3);
                statusCell.setCellValue("ABSENT");
                statusCell.setCellStyle(absentStyle);
                row.createCell(4).setCellValue("-");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AttendanceException("EXPORT_FAILED", "Failed to export Excel report: " + e.getMessage());
        }
    }

    /**
     * Generates a PDF report for an attendance session.
     */
    public byte[] generatePdfReport(Long sessionId) {
        SessionAttendanceData data = attendanceService.getSessionAttendance(sessionId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            document.add(new Paragraph("SMART ATTENDANCE MANAGEMENT SYSTEM")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("Session Attendance Report")
                    .setFontSize(13)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            // Meta Info Table
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}));
            metaTable.setWidth(UnitValue.createPercentValue(100));

            metaTable.addCell("Subject:");
            metaTable.addCell(data.getSubjectName() + " (" + data.getSubjectCode() + ")");
            metaTable.addCell("Section:");
            metaTable.addCell(data.getSectionName());

            metaTable.addCell("Faculty:");
            metaTable.addCell(data.getFacultyName());
            metaTable.addCell("Date:");
            metaTable.addCell(data.getSessionDate().toString());

            metaTable.addCell("Present Count:");
            metaTable.addCell(String.valueOf(data.getPresentCount()));
            metaTable.addCell("Absent Count:");
            metaTable.addCell(String.valueOf(data.getAbsentCount()));

            document.add(metaTable);
            document.add(new Paragraph("\n"));

            // Students Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{10, 25, 35, 15, 15}));
            table.setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell("S.No");
            table.addHeaderCell("Register No");
            table.addHeaderCell("Student Name");
            table.addHeaderCell("Status");
            table.addHeaderCell("Marked Time");

            int sNo = 1;
            for (SessionAttendanceData.StudentAttendanceInfo s : data.getPresentStudents()) {
                table.addCell(String.valueOf(sNo++));
                table.addCell(s.getRegisterNumber());
                table.addCell(s.getStudentName());
                table.addCell("PRESENT");
                table.addCell(s.getMarkedAt() != null ? s.getMarkedAt().substring(11, 19) : "-");
            }

            for (SessionAttendanceData.StudentAttendanceInfo s : data.getAbsentStudents()) {
                table.addCell(String.valueOf(sNo++));
                table.addCell(s.getRegisterNumber());
                table.addCell(s.getStudentName());
                table.addCell("ABSENT");
                table.addCell("-");
            }

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new AttendanceException("EXPORT_FAILED", "Failed to export PDF report: " + e.getMessage());
        }
    }
}

