package com.soldati.mapper;

import java.math.BigDecimal;

import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiIsoDTO;
import com.soldati.models.IsoCode;
import com.soldati.models.SonarFact;
import com.soldati.regole.ComplianceCalculator;

public final class DashboardMapper {

    // quote della categoria: entro = kpi entro soglia / kpi che concorrono, fuori idem, np = il resto
    public static IsoSintesiDTO buildIsoSintesi(IsoCode iso, long kpiCompliance, long entro, long fuori) {
        BigDecimal quotaEntro = ComplianceCalculator.quota(entro, kpiCompliance);
        BigDecimal quotaFuori = ComplianceCalculator.quota(fuori, kpiCompliance);
        return IsoSintesiDTO.builder()
                .codice(iso.getIso_code())
                .descrizione(iso.getDescription())
                .kpiCompliance(kpiCompliance)
                .entro(quotaEntro)
                .fuori(quotaFuori)
                .np(BigDecimal.ONE.subtract(quotaEntro).subtract(quotaFuori))
                .build();
    }

    // kpi del catalogo con la riga dell'ultima analisi
    public static KpiIsoDTO buildKpiIso(KpiConfigDTO kpi, SonarFact fact, String esito) {
        return KpiIsoDTO.builder()
                .chiave(kpi.getChiave())
                .nome(kpi.getNome())
                .tipoControllo(fact != null ? fact.getTipoControllo().getDescrizione() : null)
                .sogliaMin(fact != null && fact.getSogliaMin() != null ? fact.getSogliaMin() : kpi.getSogliaMin())
                .sogliaMax(fact != null && fact.getSogliaMax() != null ? fact.getSogliaMax() : kpi.getSogliaMax())
                .calcCompliance(kpi.isCalcCompliance())
                .valore(fact != null ? fact.getSonarValues() : null)
                .esito(esito)
                .build();
    }
}