package com.soldati.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.soldati.service.interfaces.ProjectService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/project")
public class ProjectController {

    private final ProjectService projectS;

    // senza parametro restituisce tutte le righe, con parametro solo quelle del progetto
    @GetMapping("/list")
    public ResponseEntity<Object> list(@RequestParam(required = false) String projectName) {
        Object r;
        HttpStatus status = HttpStatus.OK;
        try {
            r = projectS.findAll(projectName);
        } catch (Exception e) {
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(r);
    }

    // legge progetti e tag da GitLab e inserisce le coppie nuove
    @PostMapping("/sync")
    public ResponseEntity<Object> sync() {
        try {
            return ResponseEntity.ok(Map.of("nuoveRighe", projectS.sincronizzaDaGitLab()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
