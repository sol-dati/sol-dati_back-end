package com.soldati.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.GitLabProjectDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.SonarProcessDTO;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.dto.output.SonarRuleDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.SonarScanMapper;
import com.soldati.service.interfaces.CatalogoSonarService;
import com.soldati.service.interfaces.GitLabService;
import com.soldati.service.interfaces.KpiConfigService;
import com.soldati.service.interfaces.ScannerService;
import com.soldati.service.interfaces.SonarApiService;
import com.soldati.service.interfaces.SonarSaveService;
import com.soldati.service.interfaces.SonarScanService;
import com.soldati.util.FileUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class SonarScanImpl implements SonarScanService {

    // ogni quanti secondi controllare se SonarQube ha finito
    private static final long POLL_INTERVAL_SECONDS = 10;

    // numero massimo di controlli (30 x 10s = 5 minuti)
    private static final long MAX_POLL_ATTEMPTS = 30;

    private final GitLabService gitLabService;
    private final ScannerService scannerService;
    private final SonarApiService sonarApiService;
    private final SonarSaveService sonarSaveService;
    private final KpiConfigService kpiConfigService;
    private final CatalogoSonarService catalogoSonarService;

    @Override
    public SonarResultDTO avviaAnalisi(AvviaSqReq req) throws SoldatiException {

        validaRichiesta(req);

        log.debug("Avvio analisi SQ: projectName={}, release={}, sprint={}, tag={}",
                req.getProjectName(), req.getRelease(), req.getSprint(), req.getTag());

        // passo 1: progetto su GitLab
        GitLabProjectDTO gitLabProject = gitLabService.findProjectByName(req.getProjectName());

        // passo 2: sorgenti del tag scelto in una cartella temporanea
        File sourceDir = gitLabService.fetchSourceForTag(gitLabProject.getId(), req.getTag());

        // chiave identificativa del progetto su SonarQube
        String projectKey = SonarScanMapper.buildProjectKey(req);

        try {
            Date momentoAvvio = new Date();

            // passo 3: analisi (Maven o SonarScanner CLI) inviata a SonarQube
            scannerService.runSonarAnalisi(sourceDir, req.getProjectName(), projectKey);

            // passo 4: attesa dell'elaborazione lato SonarQube
            attendiCompletamentoSonar(projectKey, momentoAvvio);
            log.debug("Elaborazione SonarQube completata per projectKey={}", projectKey);

            // passo 5: tabelle di catalogo allineate con SonarQube
            catalogoSonarService.sincronizzaMetriche();
            catalogoSonarService.sincronizzaLanguage();
            catalogoSonarService.sincronizzaType();
            catalogoSonarService.sincronizzaSeverity();

            // passo 6: metriche
            Map<String, Object> resultSonar = sonarApiService.getResult(projectKey);

            // passo 7: regole violate + regole del piano qualità senza violazioni (valore 0, servono alla compliance)
            List<SonarRuleDTO> rules = costruisciRules(projectKey, sonarApiService.getIssues(projectKey));
            rules.addAll(costruisciRegoleSenzaViolazioni(projectKey, rules));

            SonarResultDTO result = SonarScanMapper.buildSonarResult(projectKey, resultSonar, rules);

            // passo 8: salvataggio su DB
            SonarProcessReq processReq = SonarScanMapper.buildSonarProcessReq(req, momentoAvvio);
            SonarProcessDTO processoSalvato = sonarSaveService.salvaAnalisi(processReq, req.getTag(), result);
            log.debug("Risultati salvati su DB: sonar_process id={}", processoSalvato.getId());

            return result;

        } finally {
            FileUtils.pulisciCartellaTemporanea(sourceDir);
        }
    }

    // campi obbligatori della richiesta
    private void validaRichiesta(AvviaSqReq req) {
        if (req.getProjectName() == null || req.getProjectName().isBlank()) {
            throw new IllegalArgumentException("projectName non può essere vuoto");
        }
        if (req.getTag() == null || req.getTag().isBlank()) {
            throw new IllegalArgumentException("tag non può essere vuoto");
        }
        if (req.getRelease() == null) {
            throw new IllegalArgumentException("release obbligatoria");
        }
    }

    // controlla a intervalli regolari se SonarQube ha finito di elaborare l'analisi
    private void attendiCompletamentoSonar(String projectKey, Date momentoAvvio) {

        for (int tentativo = 1; tentativo <= MAX_POLL_ATTEMPTS; tentativo++) {

            pausa(projectKey);

            boolean ancoraInCorso = sonarApiService.isRunning(projectKey, momentoAvvio);
            log.debug("Tentativo #{} per projectKey={}: in elaborazione={}", tentativo, projectKey, ancoraInCorso);

            if (!ancoraInCorso) {
                return;
            }
        }

        throw new SoldatiException("Timeout: SonarQube non ha completato l'elaborazione per projectKey=" + projectKey);
    }

    // attesa tra un controllo e l'altro; se il thread viene interrotto si ripristina il flag e si interrompe l'analisi
    private void pausa(String projectKey) {
        try {
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SoldatiException("Attesa di SonarQube interrotta per projectKey=" + projectKey, e);
        }
    }

    // una SonarRuleDTO per ogni regola violata, con il numero di violazioni
    private List<SonarRuleDTO> costruisciRules(String projectKey, List<Map<String, Object>> issues) {

        Map<String, Long> violazioniPerRegola = SonarScanMapper.contaViolazioniPerRegola(issues);

        List<SonarRuleDTO> rules = new ArrayList<>();
        for (Map.Entry<String, Long> voce : violazioniPerRegola.entrySet()) {
            Map<String, Object> dettagli = sonarApiService.getRuleDetails(voce.getKey());
            rules.add(SonarScanMapper.buildRule(voce.getKey(), dettagli, voce.getValue()));
        }

        log.debug("Rules costruite per projectKey={}: {} regole distinte", projectKey, rules.size());
        return rules;
    }

    // regole del catalogo kpi_compliance attive su SonarQube ma senza violazioni
    private List<SonarRuleDTO> costruisciRegoleSenzaViolazioni(String projectKey, List<SonarRuleDTO> violate) {

        List<SonarRuleDTO> regole = new ArrayList<>();

        try {
            Set<String> giaPresenti = violate.stream()
                    .map(SonarRuleDTO::getRuleKey)
                    .collect(Collectors.toSet());

            Set<String> attive = sonarApiService.getRegoleAttive(projectKey);

            for (KpiConfigDTO kpi : kpiConfigService.leggiKpiCompliance().values()) {
                String chiave = kpi.getChiave();
                if (giaPresenti.contains(chiave) || !attive.contains(chiave)) {
                    continue;
                }
                regole.add(SonarScanMapper.buildRule(chiave, sonarApiService.getRuleDetails(chiave), 0));
            }

        } catch (RuntimeException e) {
            log.warn("Regole senza violazioni non recuperate per projectKey={}: {}", projectKey, e.getMessage());
        }

        log.debug("Regole del catalogo senza violazioni per projectKey={}: {}", projectKey, regole.size());
        return regole;
    }
}