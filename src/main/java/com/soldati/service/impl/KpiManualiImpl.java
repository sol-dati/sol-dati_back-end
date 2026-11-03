package com.soldati.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.input.KpiManualiReq;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiManualeDTO;
import com.soldati.mapper.KpiManualiMapper;
import com.soldati.mapper.Mapper;
import com.soldati.models.IsoCode;
import com.soldati.models.Project;
import com.soldati.models.SeveritySonar;
import com.soldati.models.SonarFact;
import com.soldati.models.SonarProcess;
import com.soldati.models.TipoControllo;
import com.soldati.regole.ComplianceCalculator;
import com.soldati.repository.IsoCodeRepo;
import com.soldati.repository.ProjectRepo;
import com.soldati.repository.SeveritySonarRepo;
import com.soldati.repository.SonarFactRepo;
import com.soldati.repository.SonarProcessRepo;
import com.soldati.repository.TipoControlloRepo;
import com.soldati.service.interfaces.KpiConfigService;
import com.soldati.service.interfaces.KpiManualiService;
import com.soldati.service.interfaces.SogliaService;
import com.soldati.service.interfaces.TipoInserimentoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiManualiImpl implements KpiManualiService {

    private final ProjectRepo projectRepo;
    private final SonarFactRepo sonarFactRepo;
    private final SonarProcessRepo sonarProcessRepo;
    private final TipoControlloRepo tipoControlloRepo;
    private final IsoCodeRepo isoCodeRepo;
    private final SeveritySonarRepo severityRepo;
    private final KpiConfigService kpiConfigService;
    private final SogliaService sogliaService;
    private final TipoInserimentoService tipoInserimentoService;

    @Override
    @Transactional(readOnly = true)
    public List<KpiManualeDTO> elenco(String projectName, String tag) {

        Project project = trovaProject(projectName, tag);

        Map<String, String> valori = sonarFactRepo.findUltimaAnalisiId(project.getId())
                .map(id -> KpiManualiMapper.valoriDaFact(sonarFactRepo.findByProcessoAndTipo(id, TipoControllo.TIPO_MANUALE)))
                .orElse(Map.of());

        return catalogoManuali().values().stream()
                .map(kpi -> KpiManualiMapper.buildKpiManualeDTO(kpi, valori.get(kpi.getChiave())))
                .toList();
    }

    @Override
    @Transactional
    public void salva(KpiManualiReq req) {

        Project project = trovaProject(req.getProjectName(), req.getTag());

        Long processoId = sonarFactRepo.findUltimaAnalisiId(project.getId())
                .orElseThrow(() -> new IllegalStateException("Il tag " + req.getTag()
                        + " non ha ancora un'analisi SQ: esegui prima Avvia SQ"));

        Map<String, String> valori = KpiManualiMapper.valoriDaReq(req);

        sostituisciValori(processoId, project, valori);

        project.setDataInvioManuali(LocalDateTime.now());

        log.info("KPI manuali salvati per {} / {}: {} valori sull'analisi {}", req.getProjectName(), req.getTag(),
                valori.size(), processoId);
    }

    @Override
    @Transactional
    public void ricopiaSuNuovaAnalisi(String projectName, String tag, Long processoNuovo) {

        Project project = trovaProject(projectName, tag);

        sonarFactRepo.findAnalisiPrecedenteConTipo(project.getId(), TipoControllo.TIPO_MANUALE, processoNuovo)
                .ifPresent(precedente -> {
                    Map<String, String> valori = KpiManualiMapper.valoriDaFact(
                            sonarFactRepo.findByProcessoAndTipo(precedente, TipoControllo.TIPO_MANUALE));
                    sostituisciValori(processoNuovo, project, valori);
                    log.info("Ricopiati {} KPI manuali di {} / {} dall'analisi {} alla {}", valori.size(),
                            projectName, tag, precedente, processoNuovo);
                });
    }

    private Project trovaProject(String projectName, String tag) {
        if (projectName == null || projectName.isBlank() || tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("projectName e tag sono obbligatori");
        }
        return projectRepo.findByProjectNameAndTag(projectName.trim().toLowerCase(Locale.ROOT), tag)
                .orElseThrow(() -> new IllegalStateException("Tag non presente in project: " + projectName + " / " + tag));
    }

    // KPI manuali del catalogo, per chiave
    private Map<String, KpiConfigDTO> catalogoManuali() {
        Map<String, KpiConfigDTO> manuali = new LinkedHashMap<>();
        kpiConfigService.leggiKpiCompliance().forEach((chiave, kpi) -> {
            if (KpiManualiMapper.isManuale(kpi)) {
                manuali.put(chiave, kpi);
            }
        });
        return manuali;
    }

    // cancella i valori manuali dell'analisi e salva quelli nuovi, con soglie e codice ISO del catalogo attuale
    private void sostituisciValori(Long processoId, Project project, Map<String, String> valori) {

        sonarFactRepo.deleteAll(sonarFactRepo.findByProcessoAndTipo(processoId, TipoControllo.TIPO_MANUALE));

        Map<String, KpiConfigDTO> catalogo = catalogoManuali();
        SonarProcess processo = sonarProcessRepo.getReferenceById(processoId);
        TipoControllo tipo = tipoControlloRepo.findBySonarControlType(TipoControllo.TIPO_MANUALE)
                .orElseThrow(() -> new IllegalStateException("Tipo controllo non presente: " + TipoControllo.TIPO_MANUALE));
        String idTipoInserimento = tipoInserimentoService
                .findByDescrizione(Mapper.createTipoInserimento(ComplianceCalculator.INSERIMENTO_MANUALE)).getIdTipo();

        List<SonarFact> facts = new ArrayList<>();

        for (Map.Entry<String, String> v : valori.entrySet()) {

            KpiConfigDTO kpi = catalogo.get(v.getKey());
            if (kpi == null) {
                throw new IllegalArgumentException("KPI manuale non presente in kpi_compliance: " + v.getKey());
            }

            IsoCode iso = isoCodeRepo.findById(kpi.getIsoCode())
                    .orElseThrow(() -> new IllegalStateException("Codice ISO non presente in iso25010code: " + kpi.getIsoCode()));
            SeveritySonar severity = kpi.getSeverita() != null ? severityRepo.findBySeverity(kpi.getSeverita()).orElse(null) : null;
            Integer idSoglia = sogliaService
                    .findByDescrizione(Mapper.createSoglia(ComplianceCalculator.esitoSoglia(v.getValue(), kpi))).getIdSoglia();

            facts.add(KpiManualiMapper.createFact(processo, project, tipo, kpi, v.getValue(), iso, severity, idSoglia,
                    idTipoInserimento));
        }

        sonarFactRepo.saveAll(facts);
    }
}
