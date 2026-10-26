package com.soldati.mapper;

import java.math.BigDecimal;

import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.models.KpiCompliance;
import com.soldati.models.KpiOthers;

public final class KpiConfigMapper {

    public static KpiConfigDTO buildKpiCompliance(KpiCompliance k) {
        return buildKpiConfigDTO(k.getKeyKpi(), k.getKpiName(), k.getIsoCode(), k.getTargetMin(), k.getTargetMax(),
                k.getCalcCompliance(), k.getTypeKpiCod(), k.getSeverity());
    }

    public static KpiConfigDTO buildKpiOthers(KpiOthers k) {
        return buildKpiConfigDTO(k.getKeyKpi(), k.getKpiName(), k.getIsoCode(), k.getTargetMin(), k.getTargetMax(),
                k.getCalcCompliance(), k.getTypeKpiCod(), k.getSeverity());
    }

    // chiave vuota o nulla: la riga non si puo' collegare a sonar_fact
    public static boolean chiaveValida(String chiave) {
        return chiave != null && !chiave.isBlank();
    }

    private static KpiConfigDTO buildKpiConfigDTO(String chiave, String nome, String iso, 
    		BigDecimal min, BigDecimal max, Boolean calc, String tipo, String severita) {
        return KpiConfigDTO.builder()
                .chiave(chiave.trim())
                .nome(nome)
                .isoCode(iso)
                .sogliaMin(min)
                .sogliaMax(max)
                .calcCompliance(Boolean.TRUE.equals(calc))
                .tipoKpi(tipo)
                .severita(severita == null || severita.isBlank() ? null : severita.trim().toUpperCase())
                .build();
    }
}
