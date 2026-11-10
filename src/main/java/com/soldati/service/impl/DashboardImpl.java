package com.soldati.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.output.DimensioneCodiceDTO;
import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.IssueKpiDTO;
import com.soldati.dto.output.IssueSezioneDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiIsoDTO;
import com.soldati.enums.SeverityIssue;
import com.soldati.mapper.DashboardMapper;
import com.soldati.models.IsoCode;
import com.soldati.models.SonarFact;
import com.soldati.repository.IsoCodeRepo;
import com.soldati.repository.SonarFactRepo;
import com.soldati.service.interfaces.ComplianceService;
import com.soldati.service.interfaces.DashboardService;
import com.soldati.service.interfaces.KpiConfigService;

import lombok.RequiredArgsConstructor;

// dati delle sezioni della dashboard; la compliance arriva da ComplianceService (formula unica)
@Service
@RequiredArgsConstructor
public class DashboardImpl implements DashboardService {

    private final SonarFactRepo sonarFactRepo;
    private final IsoCodeRepo isoCodeRepo;
    private final KpiConfigService kpiConfigService;
    private final ComplianceService complianceService;

    @Override
    public List<IsoSintesiDTO> sintesi(Long projectId) {
        return complianceService.sintesi(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiIsoDTO> kpiIso(Long projectId, String isoCode) {

        // righe dell'ultima analisi collegate al catalogo, per chiave
        Map<String, SonarFact> righe = new HashMap<>();
        for (SonarFact f : sonarFactRepo.findUltimaAnalisi(projectId)) {
            if (f.getFkKpiCompliance() != null) {
                righe.putIfAbsent(f.getFkKpiCompliance(), f);
            }
        }

        // tutti i KPI della categoria; quelli non misurati arrivano con valore ed esito vuoti
        return kpiConfigService.leggiKpiCompliance().values().stream()
                .filter(k -> isoCode.equals(k.getIsoCode()))
                .sorted(Comparator.comparing(KpiConfigDTO::getChiave))
                .map(k -> DashboardMapper.buildKpiIso(k, righe.get(k.getChiave())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueSezioneDTO> issue(Long projectId) {

        Map<String, KpiConfigDTO> catalogo = kpiConfigService.leggiKpiCompliance();
        Map<String, String> categorie = isoCodeRepo.findAll().stream()
                .collect(Collectors.toMap(IsoCode::getIso_code, IsoCode::getDescription));

        // righe dell'ultima analisi collegate al catalogo e con una severità
        List<SonarFact> righe = sonarFactRepo.findUltimaAnalisi(projectId).stream()
                .filter(f -> f.getSeverity() != null && catalogo.containsKey(f.getFkKpiCompliance()))
                .toList();

        List<IssueSezioneDTO> sezioni = new ArrayList<>();
        for (SeverityIssue severita : SeverityIssue.values()) {
            List<IssueKpiDTO> kpi = righe.stream()
                    .filter(f -> severita.name().equals(f.getSeverity().getSeverity()))
                    .map(f -> DashboardMapper.buildIssueKpi(catalogo.get(f.getFkKpiCompliance()), f, categorie))
                    .sorted(Comparator.comparing(IssueKpiDTO::getIsoDescrizione).thenComparing(IssueKpiDTO::getNome))
                    .toList();
            sezioni.add(DashboardMapper.buildIssueSezione(severita.name(), kpi));
        }
        return sezioni;
    }
    
    @Override
    @Transactional(readOnly = true)
    public DimensioneCodiceDTO dimensioneCodice(Long projectId) {
        return DashboardMapper.buildDimensioneCodice(sonarFactRepo.findUltimaAnalisi(projectId));
    }
}