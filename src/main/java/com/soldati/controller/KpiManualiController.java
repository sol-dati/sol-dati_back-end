package com.soldati.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.soldati.dto.input.KpiManualiReq;
import com.soldati.service.interfaces.KpiManualiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/kpi-manuali")
public class KpiManualiController {

    private final KpiManualiService kpiManualiService;

    // KPI manuali del catalogo con i valori già inseriti per il tag
    @GetMapping("/list")
    public ResponseEntity<Object> list(@RequestParam String projectName, @RequestParam String tag) {
        Object r;
        HttpStatus status = HttpStatus.OK;
        try {
            r = kpiManualiService.elenco(projectName, tag);
        } catch (Exception e) {
            log.error("Errore lettura KPI manuali per {} / {}", projectName, tag, e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(r);
    }

    // salva i valori inseriti (sostituiscono quelli precedenti del tag)
    @PostMapping("/create")
    public ResponseEntity<Object> salva(@RequestBody KpiManualiReq request) {
        try {
            kpiManualiService.salva(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Errore salvataggio KPI manuali: {}", request, e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
