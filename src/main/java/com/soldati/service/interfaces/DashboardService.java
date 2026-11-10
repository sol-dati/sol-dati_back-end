package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.output.DimensioneCodiceDTO;
import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.IssueSezioneDTO;
import com.soldati.dto.output.KpiIsoDTO;

public interface DashboardService {

    // compliance per categoria iso dell'ultima analisi del tag
    List<IsoSintesiDTO> sintesi(Long projectId);

    // kpi della categoria iso con valore ed esito dell'ultima analisi del tag
    List<KpiIsoDTO> kpiIso(Long projectId, String isoCode);
    
    // kpi con severità blocker e critical dell'ultima analisi del tag
    List<IssueSezioneDTO> issue(Long projectId);
    
    // numero di file e righe di codice (anche per linguaggio) dell'ultima analisi del tag
    DimensioneCodiceDTO dimensioneCodice(Long projectId);
}