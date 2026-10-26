package com.soldati.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.mapper.KpiConfigMapper;
import com.soldati.models.KpiCompliance;
import com.soldati.models.KpiOthers;
import com.soldati.repository.KpiComplianceRepo;
import com.soldati.repository.KpiOthersRepo;
import com.soldati.service.interfaces.KpiConfigService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KpiConfigImpl implements KpiConfigService {

    private final KpiComplianceRepo kpiComplianceRepo;

    private final KpiOthersRepo kpiOthersRepo;

    @Override
    @Transactional(readOnly = true)
    public Map<String, KpiConfigDTO> leggiKpiCompliance() {

        Map<String, KpiConfigDTO> mappa = new LinkedHashMap<>();

        for (KpiCompliance k : kpiComplianceRepo.findAll()) {
            if (KpiConfigMapper.chiaveValida(k.getKeyKpi())) {
                KpiConfigDTO dto = KpiConfigMapper.buildKpiCompliance(k);
                mappa.put(dto.getChiave(), dto);
            }
        }
        return mappa;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, KpiConfigDTO> leggiKpiOthers() {

        Map<String, KpiConfigDTO> mappa = new LinkedHashMap<>();

        for (KpiOthers k : kpiOthersRepo.findAll()) {
            if (KpiConfigMapper.chiaveValida(k.getKeyKpi())) {
                KpiConfigDTO dto = KpiConfigMapper.buildKpiOthers(k);
                mappa.put(dto.getChiave(), dto);
            }
        }
        return mappa;
    }
}
