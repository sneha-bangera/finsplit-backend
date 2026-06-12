package com.finsplit.finsplit.controller;

import com.finsplit.finsplit.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final CsvExportService csvExportService;

    // GET /api/export/expenses?from=2026-01-01&to=2026-06-30
    @GetMapping("/expenses")
    public ResponseEntity<byte[]> exportExpenses(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        byte[] csv = csvExportService.exportExpenses(u.getUsername(), from, to);

        String filename = "finsplit-expenses-" + LocalDate.now() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}