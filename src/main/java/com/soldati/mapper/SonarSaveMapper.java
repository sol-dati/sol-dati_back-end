package com.soldati.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.soldati.dto.input.SonarKpiReq;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.SonarProcessDTO;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.dto.output.SonarRuleDTO;
import com.soldati.models.LanguageSonar;
import com.soldati.models.Project;
import com.soldati.models.SeveritySonar;
import com.soldati.models.SonarFact;
import com.soldati.models.SonarProcess;
import com.soldati.models.TipoControllo;
import com.soldati.models.TypeSonar;
import com.soldati.regole.MetricheDerivate;

public final class SonarSaveMapper {

    private SonarSaveMapper() {
    }

    public static List<SonarKpiReq> costruisciKpiDaMetriche(Long idProcesso, SonarResultDTO result, List<Map<String, Object>> catalogoMetriche) {

        List<SonarKpiReq> lista = new ArrayList<>();
        Map<String, Map<String, Object>> metrichePerChiave = new HashMap<>();

        for (Map<String, Object> voce : catalogoMetriche) {
            Object chiave = voce.get("key");

            if (chiave != null) {
                metrichePerChiave.put(chiave.toString(), voce);
            }
        }

        for (Map.Entry<String, String> metrica : result.getMetrics().entrySet()) {

            SonarKpiReq kpiReq = new SonarKpiReq();
            kpiReq.setSonarProcessId(idProcesso);
            kpiReq.setSonarControlType(TipoControllo.TIPO_METRICA);
            kpiReq.setSonarKey(metrica.getKey());
            kpiReq.setSonarValues(metrica.getValue());

            Map<String, Object> info = metrichePerChiave.get(metrica.getKey());

            if (info != null) {
                kpiReq.setLanguage(SonarScanMapper.valoreString(info.get("domain")));
                kpiReq.setType(SonarScanMapper.valoreString(info.get("type")));
            }
            lista.add(kpiReq);

            // linee di codice per linguaggio: "java=1200;xml=80"
            if (MetricheDerivate.CHIAVE_DISTRIBUZIONE_LOC.equals(metrica.getKey())) {
                lista.addAll(MetricheDerivate.locPerLinguaggio(idProcesso, metrica.getValue()));
            }
        }

        // complessità: complexity / functions
        SonarKpiReq complessita = MetricheDerivate.complessitaPerFunzione(idProcesso, result.getMetrics());
        if (complessita != null) {
            lista.add(complessita);
        }

        return lista;
    }

    public static List<SonarKpiReq> costruisciKpiDaRule(Long idProcesso, SonarResultDTO result) {

        List<SonarKpiReq> lista = new ArrayList<>();

        if (result.getRules() == null) {
            return lista;
        }

        for (SonarRuleDTO rule : result.getRules()) {

            SonarKpiReq kpiReq = new SonarKpiReq();
            kpiReq.setSonarProcessId(idProcesso);
            kpiReq.setSonarControlType(TipoControllo.TIPO_RULE);
            kpiReq.setSonarKey(rule.getRuleKey());
            kpiReq.setSonarValues(String.valueOf(rule.getIssuesCount()));

            // Questi tre valori li ha già la regola (li riempiamo sotto)
            kpiReq.setSeverity(rule.getSeverity());
            kpiReq.setLanguage(rule.getLanguage());
            kpiReq.setType(rule.getType());

            lista.add(kpiReq);
        }

        return lista;
    }

    public static SonarFact createFact(SonarKpiReq req, SonarProcess process, Project project,
            TipoControllo tipo, SeveritySonar severity, LanguageSonar language, TypeSonar type) {

        SonarFact fact = new SonarFact();
        fact.setSonarProcess(process);
        fact.setProject(project);
        fact.setTipoControllo(tipo);
        fact.setSeverity(severity);
        fact.setLanguage(language);
        fact.setType(type);
        fact.setSonarKey(req.getSonarKey());
        fact.setSonarValues(req.getSonarValues());
        return fact;
    }

    // collegamenti al catalogo KPI (kpi_compliance o kpi_others), soglie e tipo di inserimento
    public static void completaFact(SonarFact fact, KpiConfigDTO compliance, KpiConfigDTO others, Integer idSoglia, String idTipoInserimento) {

        fact.setFkSoglia(idSoglia);
        fact.setFkTipoInserimento(idTipoInserimento);

        if (compliance != null) {
            fact.setFkKpiCompliance(compliance.getChiave());
            fact.setSogliaMin(compliance.getSogliaMin());
            fact.setSogliaMax(compliance.getSogliaMax());
            fact.setCalcCompliance(compliance.isCalcCompliance() ? 1 : 0);
        } else {
            fact.setFkKpiOthers(others != null ? others.getChiave() : null);
            fact.setCalcCompliance(0);
        }
    }

    public static SonarProcessDTO buildSonarProcessDTO(SonarProcess process) {
        return SonarProcessDTO.builder()
                .id(process.getId())
                .projectName(process.getProjectName())
                .projectRelease(process.getProjectRelease())
                .sprint(process.getSprint())
                .processDate(process.getProcessDate())
                .build();
    }

}
