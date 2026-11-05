package com.soldati.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soldati.service.interfaces.DashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    // compliance per categoria iso dell'ultima analisi del tag
    @GetMapping("/{projectId}/sintesi")
    public ResponseEntity<Object> sintesi(@PathVariable Long projectId) {
        Object r;
        HttpStatus status = HttpStatus.OK;
        try {
            r = dashboardService.sintesi(projectId);
        } catch (Exception e) {
            log.error("Errore", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(r);
    }

    // kpi della categoria iso con valore ed esito dell'ultima analisi del tag
    @GetMapping("/{projectId}/iso/{isoCode}")
    public ResponseEntity<Object> kpiIso(@PathVariable Long projectId, @PathVariable String isoCode) {
        Object r;
        HttpStatus status = HttpStatus.OK;
        try {
            r = dashboardService.kpiIso(projectId, isoCode);
        } catch (Exception e) {
            log.error("Errore", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(r);
    }
}