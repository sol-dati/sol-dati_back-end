package com.soldati.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.input.SonarKpiReq;
import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.SonarProcessDTO;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.mapper.Mapper;
import com.soldati.mapper.SonarSaveMapper;
import com.soldati.models.IsoCode;
import com.soldati.models.LanguageSonar;
import com.soldati.models.Project;
import com.soldati.models.SeveritySonar;
import com.soldati.models.SonarFact;
import com.soldati.models.SonarProcess;
import com.soldati.models.TipoControllo;
import com.soldati.models.TypeSonar;
import com.soldati.regole.ComplianceCalculator;
import com.soldati.repository.IsoCodeRepo;
import com.soldati.repository.ProjectRepo;
import com.soldati.repository.SeveritySonarRepo;
import com.soldati.repository.SonarFactRepo;
import com.soldati.repository.SonarProcessRepo;
import com.soldati.repository.TipoControlloRepo;
import com.soldati.repository.TypeSonarRepo;
import com.soldati.service.interfaces.CatalogoSonarService;
import com.soldati.service.interfaces.KpiConfigService;
import com.soldati.service.interfaces.SogliaService;
import com.soldati.service.interfaces.SonarApiService;
import com.soldati.service.interfaces.SonarSaveService;
import com.soldati.service.interfaces.TipoInserimentoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class SonarSaveImpl implements SonarSaveService {

    private final SonarProcessRepo sonarProcessRepo;
    private final SonarApiService sonarApiService;
    private final TipoControlloRepo tipoControlloRepo;
    private final ProjectRepo projectRepo;
    private final SeveritySonarRepo severityRepo;
    private final CatalogoSonarService catalogoSonarService;
    private final TypeSonarRepo typeRepo;
    private final SonarFactRepo sonarFactRepo;
    private final IsoCodeRepo isoCodeRepo;
    private final KpiConfigService kpiConfigService;
    private final SogliaService sogliaService;
    private final TipoInserimentoService tipoInserimentoService;

    // righe KPI dell'analisi: metriche + regole
    private List<SonarKpiReq> costruisciRighe(Long idProcesso, SonarResultDTO result) {

        List<Map<String, Object>> catalogo = sonarApiService.getAvailableMetrics();

        List<SonarKpiReq> righe = new ArrayList<>();
        righe.addAll(SonarSaveMapper.costruisciKpiDaMetriche(idProcesso, result, catalogo));
        righe.addAll(SonarSaveMapper.costruisciKpiDaRule(idProcesso, result));

        return righe;
    }

    // tabella tipo_controllo per codice
    private Map<String, TipoControllo> leggiTipiControllo() {
        return tipoControlloRepo.findAll().stream()
                .collect(Collectors.toMap(TipoControllo::getSonarControlType, Function.identity()));
    }

    @Override
    @Transactional
    public SonarProcessDTO salvaAnalisi(SonarProcessReq req, String tag, SonarResultDTO result) {

        // se il salvataggio delle righe fallisce, la transazione annulla anche la riga di sonar_process
        SonarProcessDTO processo = salvaProcesso(req);

        salvaFact(processo.getId(), req.getProjectName(), tag, result);

        return processo;
    }

    // riga di sonar_process
    private SonarProcessDTO salvaProcesso(SonarProcessReq req) {

        log.debug("salva sonar process {}", req);

        SonarProcess processo = new SonarProcess();
        processo.setProjectName(req.getProjectName());
        processo.setProjectRelease(req.getProjectRelease());
        processo.setSprint(req.getSprint());
        processo.setProcessDate(req.getProcessDate());

        SonarProcess processoSalvato = sonarProcessRepo.save(processo);

        log.debug("Salvato sonar_process id={}", processoSalvato.getId());

        return SonarSaveMapper.buildSonarProcessDTO(processoSalvato);
    }

    @Transactional
    @Override
    public void salvaFact(Long processId, String projectName, String tag, SonarResultDTO result) {

        SonarProcess processo = sonarProcessRepo.findById(processId)
                .orElseThrow(() -> new IllegalStateException("sonar_process non trovato: " + processId));

        // riga project di riferimento (nome e tag arrivano dal frontend)
        Project project = projectRepo.findByProjectNameAndTag(projectName.trim().toLowerCase(Locale.ROOT), tag)
                .orElseThrow(() -> new IllegalStateException("Project non trovato: " + projectName + " / " + tag));

        List<SonarKpiReq> righe = costruisciRighe(processo.getId(), result);

        // tabelle di decodifica lette una volta sola
        Map<String, TipoControllo> tipi = leggiTipiControllo();

        Map<String, SeveritySonar> severities = severityRepo.findAll().stream()
                .collect(Collectors.toMap(SeveritySonar::getSeverity, Function.identity()));

        Map<String, LanguageSonar> languages = catalogoSonarService.leggiLanguage(righe);

        Map<String, TypeSonar> types = typeRepo.findAll().stream()
                .collect(Collectors.toMap(TypeSonar::getType, Function.identity()));

        Map<String, IsoCode> isoLette = new HashMap<>();

        // cataloghi KPI (tabelle kpi_compliance e kpi_others): categoria ISO, soglie, compliance
        Map<String, KpiConfigDTO> kpiCompliance = kpiConfigService.leggiKpiCompliance();
        Map<String, KpiConfigDTO> kpiOthers = kpiConfigService.leggiKpiOthers();

        // descrizione -> id delle tabelle soglie e tipo_inserimento (letti una volta per analisi)
        Map<String, Integer> idSoglie = new HashMap<>();
        Map<String, String> idTipiInserimento = new HashMap<>();

        List<SonarFact> facts = new ArrayList<>();

        // chiavi di Sonar che non sono in nessuno dei due cataloghi: non vengono salvate
        Set<String> scartate = new LinkedHashSet<>();

        for (SonarKpiReq r : righe) {

            TipoControllo tipo = tipi.get(r.getSonarControlType());
            if (tipo == null) {
                throw new IllegalStateException("Tipo controllo non presente: " + r.getSonarControlType());
            }

            // per le metriche il "language" è il domain (Size, Security...), non un linguaggio: va a NULL
            LanguageSonar language = r.getLanguage() != null ? languages.get(r.getLanguage()) : null;

            TypeSonar type = r.getType() != null ? types.get(r.getType()) : null;

            // valore presente su Sonar ma assente dalle tabelle: non blocco il salvataggio, lo segnalo
            if (r.getType() != null && type == null) {
                log.warn("Type non in type: {} (key={})", r.getType(), r.getSonarKey());
            }
            if (r.getLanguage() != null && language == null) {
                log.warn("Language non in language: {} (key={})", r.getLanguage(), r.getSonarKey());
            }

            KpiConfigDTO compliance = kpiCompliance.get(r.getSonarKey());
            KpiConfigDTO others = kpiOthers.get(r.getSonarKey());

            // come la vecchia fact_kpi_25010: si salvano solo le chiavi presenti in kpi_compliance o kpi_others
            if (compliance == null && others == null) {
                scartate.add(r.getSonarKey());
                continue;
            }

            // severita': quella del piano qualita' (kpi_compliance.severity_c) se impostata, altrimenti quella di Sonar
            String codiceSeverita = compliance != null && compliance.getSeverita() != null ? compliance.getSeverita() : r.getSeverity();

            SeveritySonar severity = codiceSeverita != null ? severities.get(codiceSeverita) : null;

            if (codiceSeverita != null && severity == null) {
                log.warn("Severity non in severity: {} (key={})", codiceSeverita, r.getSonarKey());
            }

            SonarFact fact = SonarSaveMapper.createFact(r, processo, project, tipo, severity, language, type);

            // codice ISO dal catalogo: kpi_compliance, altrimenti kpi_others
            String codiceIso = ComplianceCalculator.codiceIso(compliance, others);

            fact.setIso_code(isoLette.computeIfAbsent(codiceIso, c -> isoCodeRepo.findById(c)
                    .orElseThrow(() -> new IllegalStateException("Codice ISO non presente in iso25010code: " + c))));

            // id di soglie e tipo_inserimento letti dal database per descrizione (una volta per analisi)
            Integer idSoglia = idSoglie.computeIfAbsent(ComplianceCalculator.esitoSoglia(r.getSonarValues(), compliance),
                    d -> sogliaService.findByDescrizione(Mapper.createSoglia(d)).getIdSoglia());

            String idTipoInserimento = idTipiInserimento.computeIfAbsent(ComplianceCalculator.tipoInserimento(compliance),
                    d -> tipoInserimentoService.findByDescrizione(Mapper.createTipoInserimento(d)).getIdTipo());

            SonarSaveMapper.completaFact(fact, compliance, others, idSoglia, idTipoInserimento);

            facts.add(fact);
        }

        sonarFactRepo.saveAll(facts);

        log.debug("Salvate {} righe in sonar_fact per process id={}", facts.size(), processo.getId());

        if (!scartate.isEmpty()) {
            log.info("Chiavi non presenti in kpi_compliance/kpi_others, non salvate: {}", scartate);
        }
    }

    @Override
    public SonarResultDTO getUltimoRisultato(Long projectId) {

        List<SonarFact> facts = sonarFactRepo.findUltimaAnalisi(projectId);

        if (facts.isEmpty()
                || !SonarSaveMapper.eDiOggi(
                        facts.get(0).getSonarProcess().getProcessDate())) {

            return SonarResultDTO.builder()
                    .metrics(new LinkedHashMap<>())
                    .rules(new ArrayList<>())
                    .iso(new ArrayList<>())
                    .build();
        }

        return SonarSaveMapper.buildRisultato(facts);
    }
}
