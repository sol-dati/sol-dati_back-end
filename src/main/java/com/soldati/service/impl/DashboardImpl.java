package com.soldati.service.impl;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiIsoDTO;
import com.soldati.mapper.DashboardMapper;
import com.soldati.models.Soglia;
import com.soldati.models.SonarFact;
import com.soldati.repository.SogliaRepo;
import com.soldati.repository.SonarFactRepo;
import com.soldati.service.interfaces.ComplianceService;
import com.soldati.service.interfaces.DashboardService;
import com.soldati.service.interfaces.KpiConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardImpl implements DashboardService {

    private final SonarFactRepo sonarFactRepo;
    private final SogliaRepo sogliaRepo;
    private final KpiConfigService kpiConfigService;
    private final ComplianceService complianceService;

    @Override
    public List<IsoSintesiDTO> sintesi(Long projectId) {
        return complianceService.sintesi(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KpiIsoDTO> kpiIso(Long projectId, String isoCode) {

        Map<Integer, String> soglie = sogliaRepo.findAll().stream()
                .collect(Collectors.toMap(Soglia::getIdSoglia, Soglia::getDescrizione));

        // righe dell'ultima analisi collegate al catalogo, per chiave
        Map<String, SonarFact> righe = new HashMap<>();
        for (SonarFact f : sonarFactRepo.findUltimaAnalisi(projectId)) {
            if (f.getFkKpiCompliance() != null) {
                righe.putIfAbsent(f.getFkKpiCompliance(), f);
            }
        }

        return kpiConfigService.leggiKpiCompliance().values().stream()
                .filter(k -> isoCode.equals(k.getIsoCode()))
                .sorted(Comparator.comparing(KpiConfigDTO::getChiave))
                .map(k -> {
                    SonarFact f = righe.get(k.getChiave());
                    return DashboardMapper.buildKpiIso(k, f, f != null ? soglie.get(f.getFkSoglia()) : null);
                })
                .toList();
    }
}