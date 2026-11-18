package com.soldati.configuration;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.soldati.service.interfaces.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// operazione del bottone "Aggiorna da GitLab" automatica
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncGitLab {

    private final ProjectService projectService;

    @Scheduled(cron = "${gitlab.sync.cron:0 0 7-19 * * *}")
    public void sincronizza() {
        try {
            int nuove = projectService.sincronizzaDaGitLab();
            log.info("Sincronizzazione GitLab programmata: {} nuovi tag", nuove);
        } catch (RuntimeException e) {
            // GitLab irraggiungibile o sincronizzazione già in corso: si riprova al giro successivo
            log.warn("Sincronizzazione GitLab programmata non eseguita: {}", e.getMessage());
        }
    }
}