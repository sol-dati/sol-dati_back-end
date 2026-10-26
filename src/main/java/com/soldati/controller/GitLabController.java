package com.soldati.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.soldati.service.interfaces.GitLabService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/rest/gitlab")
public class GitLabController {

    private final GitLabService gitLabService;

    @GetMapping("/projects")
    public ResponseEntity<Object> getProjects() {

        Object r;
        HttpStatus status = HttpStatus.OK;

        try {
            r = gitLabService.getProjectNames();
        } catch (Exception e) {
            log.error("Errore durante il recupero dei progetti GitLab", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(r);
    }
    
 // Restituisce i tag del progetto scelto nella select
    @GetMapping("/projects/{projectName}/tags")
    public ResponseEntity<Object> getTags(@PathVariable String projectName) {

        Object r;
        HttpStatus status = HttpStatus.OK;

        try {
            r = gitLabService.getTags(projectName);
        } catch (Exception e) {
            log.error("Errore durante il recupero dei tag GitLab", e);
            r = e.getMessage();
            status = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(status).body(r);
    }
}