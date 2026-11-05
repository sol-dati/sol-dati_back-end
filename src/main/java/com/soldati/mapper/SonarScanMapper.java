package com.soldati.mapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.dto.output.SonarRuleDTO;

public final class SonarScanMapper {

    private SonarScanMapper() {
    }

    @SuppressWarnings("unchecked")
    public static SonarResultDTO buildSonarResult(String projectKey, Map<String, Object> resultSonar, List<SonarRuleDTO> rules) {

        Map<String, Object> component = (Map<String, Object>) resultSonar.get("component");

        if (component == null) {
            throw new IllegalStateException("Risposta SonarQube senza campo component. projectKey=" + projectKey);
        }

        Map<String, String> values = new HashMap<>();

        Object measuresObject = component.get("measures");

        if (measuresObject instanceof List<?> measures) {

            for (Object measureObject : measures) {

                if (!(measureObject instanceof Map<?, ?> measure)) {
                    continue;
                }

                String metric = valoreString(measure.get("metric"));
                String value = valoreString(measure.get("value"));

                if (value == null) {
                    value = leggiValoreDaPeriodo(measure);
                }

                if (metric != null && value != null) {
                    values.put(metric, value);
                }
            }
        }

        return SonarResultDTO.builder()
                .metrics(values)
                .rules(rules)
                .build();
    }

    private static String leggiValoreDaPeriodo(Map<?, ?> measure) {

        if (measure.get("period") instanceof Map<?, ?> period) {
            return valoreString(period.get("value"));
        }

        if (measure.get("periods") instanceof List<?> periods && !periods.isEmpty() && periods.get(0) instanceof Map<?, ?> primoPeriodo) {
            return valoreString(primoPeriodo.get("value"));
        }

        return null;
    }

    public static String valoreString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public static String buildProjectKey(AvviaSqReq req) {
        String chiave = req.getProjectName() + "-R" + req.getRelease();
        return req.getSprint() != null ? chiave + "-S" + req.getSprint() : chiave;
    }

    // riga di sonar_process da salvare: dati della richiesta + istante di avvio dell'analisi
    public static SonarProcessReq buildSonarProcessReq(AvviaSqReq req, Date momentoAvvio) {

        SonarProcessReq processReq = new SonarProcessReq();
        processReq.setProjectName(req.getProjectName());
        processReq.setProjectRelease(req.getRelease());
        processReq.setSprint(req.getSprint());
        processReq.setProcessDate(LocalDateTime.ofInstant(momentoAvvio.toInstant(), ZoneId.systemDefault()));

        return processReq;
    }

    public static Map<String, Long> contaViolazioniPerRegola(List<Map<String, Object>> issues) {

        Map<String, Long> violazioniPerRegola = new LinkedHashMap<>();

        for (Map<String, Object> issue : issues) {
            Object rule = issue.get("rule");
            if (rule == null) {
                continue;
            }
            violazioniPerRegola.merge(rule.toString(), 1L, Long::sum);
        }

        return violazioniPerRegola;
    }

    public static SonarRuleDTO buildRule(String ruleKey, Map<String, Object> dettagli, long numeroViolazioni) {

        return SonarRuleDTO.builder()
                .ruleKey(ruleKey)
                .ruleName(valoreString(dettagli.get("name")))
                .type(valoreString(dettagli.get("type")))
                .severity(valoreString(dettagli.get("severity")))
                .description(valoreString(dettagli.get("htmlDesc")))
                .issuesCount(numeroViolazioni)
                .language(valoreString(dettagli.get("lang")))
                .build();
    }
}
