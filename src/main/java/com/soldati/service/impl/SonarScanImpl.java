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
import com.soldati.mapper.SonarScanMapper;
import com.soldati.service.interfaces.CatalogoSonarService;
import com.soldati.service.interfaces.KpiConfigService;
import com.soldati.service.interfaces.GitLabService;
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
public class SonarScanImpl implements SonarScanService{
	
	// Ogni quanti secondi controllare se SonarQube ha finito
    private static final long POLL_INTERVAL_SECONDS = 10;

    // Numero massimo di controlli 
    private static final long MAX_POLL_ATTEMPTS = 30;
	
	private final GitLabService gitLabService;
    private final ScannerService scannerService;
    private final SonarApiService sonarApiService;
    private final SonarSaveService sonarSaveService;
    private final KpiConfigService kpiConfigService;
    private final CatalogoSonarService catalogoSonarService;
	
	@Override
	public SonarResultDTO avviaAnalisi(AvviaSqReq req) throws Exception {
		
		if (req.getProjectName() == null || req.getProjectName().isBlank()) {
            throw new IllegalArgumentException("projectName non può essere vuoto");
        }
        if (req.getTag() == null || req.getTag().isBlank()) {
            throw new IllegalArgumentException("tag non può essere vuoto");
        }
        
        if (req.getRelease() == null) {
            throw new IllegalArgumentException("release obbligatoria");
        }

        log.debug("Avvio analisi SQ: projectName={}, release={}, sprint={}, tag={}",
                req.getProjectName(), req.getRelease(), req.getSprint(), req.getTag());
        
        // Passo 1: troviamo il progetto su GitLab 
        GitLabProjectDTO gitLabProject = gitLabService.findProjectByName(req.getProjectName());
       

        // Passo 2: scarichiamo i sorgenti del tag scelto
        File sourceDir = gitLabService.fetchSourceForTag(gitLabProject.getId(), req.getTag());

        // richiama metodo che crea la chiave identificativa del progetto su SonarQube
        String projectKey = SonarScanMapper.buildProjectKey(req);

        try {
        	//istante in cui si avvia l'analisi
            Date momentoAvvio  = new Date();

            // Passo 3: lanciamo maven, che a sua volta invia l'analisi a SonarQube
            scannerService.runSonarAnalisi(sourceDir, projectKey);

            log.debug("Maven/Sonar completato, in attesa che SonarQube finisca di elaborare projectKey={}", projectKey);
            
         // Passo 4: aspettiamo che SonarQube finisca
            waitCompletamentoSonar(projectKey, momentoAvvio);

            log.debug("Elaborazione SonarQube completata per projectKey={}", projectKey);
            
            catalogoSonarService.sincronizzaMetriche();
            catalogoSonarService.sincronizzaLanguage();
            catalogoSonarService.sincronizzaType();
            catalogoSonarService.sincronizzaSeverity();

            // Passo 6: recuperiamo i risultati
            Map<String, Object> resultSonar =  sonarApiService.getResult(projectKey);

            log.debug("Metriche SonarQube recuperate per projectKey={}", projectKey);


            // Passo 7: recuperiamo tutte le issue
            List<Map<String, Object>> issues = sonarApiService.getIssues(projectKey);


         // Passo 7b: costruiamo la lista delle rules (una sola volta per regola distinta)
            List<SonarRuleDTO> rules = costruisciRules(projectKey, issues);

            // Passo 7c: regole del piano qualità (kpi_compliance) attive su Sonar ma senza violazioni,
            // salvate con valore 0 servono per il calcolo della compliance
            rules.addAll(costruisciRegoleSenzaViolazioni(projectKey, rules));

            SonarResultDTO result = SonarScanMapper.buildSonarResult(projectKey, resultSonar, rules);
            
            // Passo 8: salviamo i risultati sul DB
            SonarProcessReq processReq = SonarScanMapper.buildSonarProcessReq(req, momentoAvvio);

            SonarProcessDTO processoSalvato = sonarSaveService.salvaRisultati(processReq, result);

            log.debug("Risultati salvati su DB: sonar_process id={}", processoSalvato.getId());
            
            sonarSaveService.salvaFact(processoSalvato.getId(), req.getProjectName(), req.getTag(), result);

            return result;

        } finally {
        	FileUtils.pulisciCartellaTemporanea(sourceDir);
        }

    }
	
    //Controlla ripetutamente se SonarQube ha finito di elaborare

    private void waitCompletamentoSonar(String projectKey, Date momentoAvvio) throws Exception {

        for (int tentativo = 1; tentativo <= MAX_POLL_ATTEMPTS; tentativo++) {

            // Aspettiamo qualche secondo prima di ogni controllo
        	
            TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);

            boolean ancoraInCorso = sonarApiService.isRunning(projectKey, momentoAvvio);

            log.debug("Tentativo #{} per projectKey={}: sonar qube in elaborazione={}", tentativo, projectKey, ancoraInCorso);

            if (!ancoraInCorso) {
                return;
            }
        }

        // abbiamo superato il numero massimo di tentativi
        throw new IllegalStateException("Timeout: SonarQube non ha completato l'elaborazione entro il limite atteso per projectKey=" + projectKey);
    }

    private List<SonarRuleDTO> costruisciRules(String projectKey, List<Map<String, Object>> issues) throws Exception {

        Map<String, Long> violazioniPerRegola = SonarScanMapper.contaViolazioniPerRegola(issues);

        List<SonarRuleDTO> rules = new ArrayList<>();

        for (Map.Entry<String, Long> voce : violazioniPerRegola.entrySet()) {
            Map<String, Object> dettagli = sonarApiService.getRuleDetails(voce.getKey());
            rules.add(SonarScanMapper.buildRule(voce.getKey(), dettagli, voce.getValue()));
        }

        log.debug("Rules costruite per projectKey={}: {} regole distinte", projectKey, rules.size());

        return rules;
    }

    private List<SonarRuleDTO> costruisciRegoleSenzaViolazioni(String projectKey, List<SonarRuleDTO> violate) {

        List<SonarRuleDTO> regole = new ArrayList<>();

        try {
            Set<String> giaPresenti = violate.stream()
                    .map(SonarRuleDTO::getRuleKey)
                    .collect(Collectors.toSet());

            Set<String> attive = sonarApiService.getRegoleAttive(projectKey);

            for (KpiConfigDTO kpi : kpiConfigService.leggiKpiCompliance().values()) {

                String chiave = kpi.getChiave();

                // tutte le regole del catalogo (anche calc_compliance_c = 0, che vengono solo mostrate)
                if (giaPresenti.contains(chiave) || !attive.contains(chiave)) {
                    continue;
                }

                Map<String, Object> dettagli = sonarApiService.getRuleDetails(chiave);
                regole.add(SonarScanMapper.buildRule(chiave, dettagli, 0));
            }

        } catch (Exception e) {
            log.warn("Regole senza violazioni non recuperate per projectKey={}: {}", projectKey, e.getMessage());
        }

        log.debug("Regole del catalogo senza violazioni per projectKey={}: {}", projectKey, regole.size());

        return regole;
    }
}
