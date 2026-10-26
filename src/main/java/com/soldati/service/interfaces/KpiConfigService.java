package com.soldati.service.interfaces;

import java.util.Map;

import com.soldati.dto.output.KpiConfigDTO;

public interface KpiConfigService {

    Map<String, KpiConfigDTO> leggiKpiCompliance();

    Map<String, KpiConfigDTO> leggiKpiOthers();
}
