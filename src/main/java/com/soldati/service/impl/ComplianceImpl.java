package com.soldati.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.enums.EsitoSoglia;
import com.soldati.mapper.DashboardMapper;
import com.soldati.models.IsoCode;
import com.soldati.models.SonarFact;
import com.soldati.regole.ComplianceCalculator;
import com.soldati.repository.IsoCodeRepo;
import com.soldati.repository.SonarFactRepo;
import com.soldati.service.interfaces.ComplianceService;
import com.soldati.service.interfaces.KpiConfigService;

import lombok.RequiredArgsConstructor;

// compliance del piano qualità con le regole in ComplianceCalculator, usata sia per il totale dell'elenco sia per le quote per categoria
@Service
@RequiredArgsConstructor
public class ComplianceImpl implements ComplianceService {

    private final SonarFactRepo sonarFactRepo;
    private final IsoCodeRepo isoCodeRepo;
    private final KpiConfigService kpiConfigService;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> compliancePerTag() {

        Map<String, KpiConfigDTO> catalogo = kpiConfigService.leggiKpiCompliance();
        List<String> categorie = categorieIso(catalogo).stream().map(IsoCode::getIso_code).toList();

        Map<Long, List<SonarFact>> righePerTag = sonarFactRepo.findUltimeAnalisiCompliance().stream()
                .collect(Collectors.groupingBy(f -> f.getProject().getId()));

        Map<Long, BigDecimal> compliance = new HashMap<>();
        righePerTag.forEach((projectId, righe) ->
                compliance.put(projectId, ComplianceCalculator.compliance(righe, catalogo, categorie)));
        return compliance;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IsoSintesiDTO> sintesi(Long projectId) {

        Map<String, KpiConfigDTO> catalogo = kpiConfigService.leggiKpiCompliance();
        List<SonarFact> righe = sonarFactRepo.findUltimaAnalisi(projectId);

        List<IsoSintesiDTO> sintesi = new ArrayList<>();
        for (IsoCode iso : categorieIso(catalogo)) {
            String codice = iso.getIso_code();
            sintesi.add(DashboardMapper.buildIsoSintesi(iso,
                    ComplianceCalculator.kpiCompliance(catalogo, codice),
                    ComplianceCalculator.contaEsito(righe, catalogo, codice, EsitoSoglia.ENTRO),
                    ComplianceCalculator.contaEsito(righe, catalogo, codice, EsitoSoglia.FUORI)));
        }
        return sintesi;
    }

    // categorie iso presenti nel catalogo, senza Extra iso, in ordine di descrizione
    private List<IsoCode> categorieIso(Map<String, KpiConfigDTO> catalogo) {
        Set<String> codici = catalogo.values().stream().map(KpiConfigDTO::getIsoCode).collect(Collectors.toSet());
        return isoCodeRepo.findAll().stream()
                .filter(i -> codici.contains(i.getIso_code()))
                .filter(i -> !ComplianceCalculator.CODICE_ISO_EXTRA.equals(i.getIso_code()))
                .sorted(Comparator.comparing(IsoCode::getDescription))
                .toList();
    }
}