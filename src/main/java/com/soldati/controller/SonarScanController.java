package com.soldati.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.service.interfaces.SonarApiService;
import com.soldati.service.interfaces.SonarSaveService;
import com.soldati.service.interfaces.SonarScanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/sonar")
public class SonarScanController {

    private final SonarScanService sonarService;
    private final SonarApiService apiService;
    private final SonarSaveService sonarSaveService;

    @PostMapping("/avvia")
    public ResponseEntity<Object> avviaAnalisi(@RequestBody AvviaSqReq request) {

        Object r;
        HttpStatus status = HttpStatus.OK;

        try {
            r = sonarService.avviaAnalisi(request);
        } catch (Exception e) {
            log.error("Errore durante l'avvio dell'analisi SQ", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(r);
    }

    @GetMapping("/status")
    public ResponseEntity<Object> checkStatus(@RequestParam String projectKey) {

        Object r;
        HttpStatus status = HttpStatus.OK;

        try {
            boolean running = apiService.isRunning(projectKey, new Date(0));
            r = "running=" + running;
        } catch (Exception e) {
            log.error("Errore durante il controllo stato SQ", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(r);
    }

    @GetMapping("/fact/{projectId}")
    public ResponseEntity<Object> risultato(@PathVariable Long projectId) {
        try {
            return ResponseEntity.ok(sonarSaveService.getUltimoRisultato(projectId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
