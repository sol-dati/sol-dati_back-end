package com.soldati.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.soldati.dto.input.KpiManualeReq;
import com.soldati.dto.input.KpiManualiReq;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiManualeDTO;
import com.soldati.models.IsoCode;
import com.soldati.models.Project;
import com.soldati.models.SeveritySonar;
import com.soldati.models.SonarFact;
import com.soldati.models.SonarProcess;
import com.soldati.models.TipoControllo;
import com.soldati.regole.ComplianceCalculator;

public final class KpiManualiMapper {

    // codice di kpi_compliance.type_kpi_cod per i KPI inseriti a mano
    public static final String TIPO_KPI_MANUALE = "M";

    private KpiManualiMapper() {
    }

    public static boolean isManuale(KpiConfigDTO kpi) {
        return kpi.getTipoKpi() != null && TIPO_KPI_MANUALE.equals(kpi.getTipoKpi().trim());
    }

    public static KpiManualeDTO buildKpiManualeDTO(KpiConfigDTO kpi, String valore) {
        return KpiManualeDTO.builder()
                .chiave(kpi.getChiave())
                .nome(kpi.getNome())
                .descrizione(kpi.getDescrizione())
                .isoCode(kpi.getIsoCode())
                .sogliaMin(kpi.getSogliaMin())
                .sogliaMax(kpi.getSogliaMax())
                .calcCompliance(kpi.isCalcCompliance())
                .valore(valore)
                .build();
    }

    // chiave -> valore delle righe di sonar_fact
    public static Map<String, String> valoriDaFact(List<SonarFact> facts) {
        Map<String, String> valori = new LinkedHashMap<>();
        for (SonarFact f : facts) {
            valori.put(f.getSonarKey(), f.getSonarValues());
        }
        return valori;
    }

    // chiave -> valore dalla richiesta: i vuoti si scartano, i non numerici sono un errore
    public static Map<String, String> valoriDaReq(KpiManualiReq req) {
        Map<String, String> valori = new LinkedHashMap<>();
        for (KpiManualeReq v : req.getValori()) {
            if (v.getChiave() == null || v.getValore() == null || v.getValore().isBlank()) {
                continue;
            }
            String valore = v.getValore().trim();
            if (!ComplianceCalculator.numeroValido(valore)) {
                throw new IllegalArgumentException("Valore non numerico per il KPI " + v.getChiave() + ": " + valore);
            }
            valori.put(v.getChiave().trim(), valore.replace(',', '.'));
        }
        return valori;
    }

    // riga di sonar_fact per un valore manuale; soglia e tipo di inserimento arrivano già risolti dal DB
    public static SonarFact createFact(SonarProcess processo, Project project, TipoControllo tipo, KpiConfigDTO kpi,
            String valore, IsoCode iso, SeveritySonar severity, Integer idSoglia, String idTipoInserimento) {

        SonarFact fact = new SonarFact();
        fact.setSonarProcess(processo);
        fact.setProject(project);
        fact.setTipoControllo(tipo);
        fact.setSeverity(severity);
        fact.setIso_code(iso);
        fact.setSonarKey(kpi.getChiave());
        fact.setSonarValues(valore);

        SonarSaveMapper.completaFact(fact, kpi, null, idSoglia, idTipoInserimento);
        return fact;
    }
}
