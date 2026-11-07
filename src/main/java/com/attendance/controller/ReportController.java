package com.attendance.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.attendance.dto.AttendanceSummary;
import com.attendance.dto.SessionAttendanceData;
import com.attendance.entity.AttendanceRecord;
import com.attendance.repository.AttendanceRecordRepository;
import com.attendance.service.AttendanceService;
import com.attendance.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AttendanceService attendanceService;
    private final AttendanceRecordRepository recordRepository;

    public ReportController(ReportService reportService,
                            AttendanceService attendanceService,
                            AttendanceRecordRepository recordRepository) {
        this.reportService = reportService;
        this.attendanceService = attendanceService;
        this.recordRepository = recordRepository;
    }

    @GetMapping("/attendance/{sessionId}")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<SessionAttendanceData> getSessionReport(@PathVariable("sessionId") Long sessionId) {
        return ResponseEntity.ok(attendanceService.getSessionAttendance(sessionId));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'ADMIN')")
    public ResponseEntity<List<AttendanceRecord>> getStudentReport(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(recordRepository.findByStudentStudentId(studentId));
    }

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('STUDENT', 'FACULTY', 'ADMIN')")
    public ResponseEntity<List<AttendanceSummary>> getStudentSummary(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(studentId));
    }

    @GetMapping("/low-attendance")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getLowAttendanceStudents(
            @RequestParam(value = "threshold", required = false) Double threshold,
            @RequestParam(value = "sectionId", required = false) Long sectionId) {
        return ResponseEntity.ok(reportService.getLowAttendanceStudents(sectionId, threshold));
    }

    @GetMapping("/export/excel")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam("sessionId") Long sessionId) {
        byte[] excelBytes = reportService.generateExcelReport(sessionId);
        String filename = "attendance_session_" + sessionId + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
    public ResponseEntity<byte[]> exportPdf(@RequestParam("sessionId") Long sessionId) {
        byte[] pdfBytes = reportService.generatePdfReport(sessionId);
        String filename = "attendance_session_" + sessionId + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}

