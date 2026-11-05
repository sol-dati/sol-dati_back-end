package com.soldati.service.interfaces;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.soldati.dto.output.IsoSintesiDTO;

public interface ComplianceService {

    // compliance del piano qualità dell'ultima analisi di ogni tag analizzato: id project -> da 0 a 1
    Map<Long, BigDecimal> compliancePerTag();

    // quote entro soglia / fuori soglia / NP per categoria ISO dell'ultima analisi del tag
    List<IsoSintesiDTO> sintesi(Long projectId);
}
