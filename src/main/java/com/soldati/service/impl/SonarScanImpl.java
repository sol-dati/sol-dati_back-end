package com.soldati.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.GitLabProjectDTO;
import com.soldati.dto.output.SonarProcessDTO;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.dto.output.SonarRuleDTO;
import com.soldati.enums.StatoAnalisi;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.SonarScanMapper;
import com.soldati.service.interfaces.CatalogoSonarService;
import com.soldati.service.interfaces.GitLabService;
import com.soldati.service.interfaces.KpiConfigService;
import com.soldati.service.interfaces.ProjectService;
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
	
    @Value("${sonar.attesa.intervallo.secondi:10}")
    private long intervalloSecondi;

    @Value("${sonar.attesa.tentativi:30}")
    private int tentativi;

    private final GitLabService gitLabService;
    private final ScannerService scannerService;
    private final SonarApiService sonarApiService;
    private final SonarSaveService sonarSaveService;
    private final KpiConfigService kpiConfigService;
    private final CatalogoSonarService catalogoSonarService;
    private final ProjectService projectService;

    @Override
    public SonarResultDTO avviaAnalisi(AvviaSqReq req) throws SoldatiException {

        SonarScanMapper.validaRichiesta(req);

        // stato del tag: in corso -> elaborato / errore 
        projectService.aggiornaStato(req.getProjectName(), req.getTag(), StatoAnalisi.IN_CORSO);

        boolean completata = false;
        try {
            SonarResultDTO result = eseguiAnalisi(req);
            projectService.aggiornaStato(req.getProjectName(), req.getTag(), StatoAnalisi.ELABORATO);
            completata = true;
            return result;

        } finally {
        	//il tag non resta "In corso"
            if (!completata) {
                segnaErrore(req);
            }
        }
    }

    // GitLab -> analisi -> attesa SonarQube -> cataloghi -> risultati -> salvataggio su DB
    private SonarResultDTO eseguiAnalisi(AvviaSqReq req) {

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

    // stato errore sul tag
    private void segnaErrore(AvviaSqReq req) {
        try {
            projectService.aggiornaStato(req.getProjectName(), req.getTag(), StatoAnalisi.ERRORE);
        } catch (RuntimeException ex) {
            log.warn("Stato di errore non salvato per {} / {}: {}", req.getProjectName(), req.getTag(), ex.getMessage());
        }
    }

    // controlla a intervalli regolari se SonarQube ha finito di elaborare l'analisi
    private void attendiCompletamentoSonar(String projectKey, Date momentoAvvio) {

        for (int tentativo = 1; tentativo <= tentativi; tentativo++) {

            pausa(projectKey);

            boolean ancoraInCorso = sonarApiService.isRunning(projectKey, momentoAvvio);
            log.debug("Tentativo #{} per projectKey={}: in elaborazione={}", tentativo, projectKey, ancoraInCorso);

            if (!ancoraInCorso) {
                return;
            }
        }

        throw new SoldatiException("Timeout: SonarQube non ha completato l'elaborazione per projectKey=" + projectKey);
    }

    // attesa tra un controllo e l'altro
    private void pausa(String projectKey) {
        try {
            TimeUnit.SECONDS.sleep(intervalloSecondi);
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
            List<String> chiavi = SonarScanMapper.regoleSenzaViolazioni(
                    kpiConfigService.leggiKpiCompliance().values(), violate, sonarApiService.getRegoleAttive(projectKey));

            for (String chiave : chiavi) {
                regole.add(SonarScanMapper.buildRule(chiave, sonarApiService.getRuleDetails(chiave), 0));
            }

        } catch (RuntimeException e) {
            log.warn("Regole senza violazioni non recuperate per projectKey={}: {}", projectKey, e.getMessage());
        }

        log.debug("Regole del catalogo senza violazioni per projectKey={}: {}", projectKey, regole.size());
        return regole;
    }
}