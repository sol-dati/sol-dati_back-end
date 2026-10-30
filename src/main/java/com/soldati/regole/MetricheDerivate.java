package com.soldati.regole;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.soldati.dto.input.SonarKpiReq;
import com.soldati.models.TipoControllo;

// metriche calcolate a partire da quelle di Sonar: complessità per funzione e LOC per linguaggio
public final class MetricheDerivate {

    public static final String CHIAVE_COMPLESSITA_FUNZIONE = "function_complexity";

    public static final String CHIAVE_DISTRIBUZIONE_LOC = "ncloc_language_distribution";
    public static final String CHIAVE_LOC_LINGUAGGIO = "ncloc_language";

    private MetricheDerivate() {
    }

    // complessità: complexity / functions
    public static SonarKpiReq complessitaPerFunzione(Long idProcesso, Map<String, String> metriche) {

        if (metriche.containsKey(CHIAVE_COMPLESSITA_FUNZIONE)) {
            return null;   // Sonar la fornisce già
        }

        try {
            double complessita = Double.parseDouble(metriche.get("complexity"));
            double funzioni = Double.parseDouble(metriche.get("functions"));
            if (funzioni <= 0) {
                return null;
            }

            SonarKpiReq riga = new SonarKpiReq();
            riga.setSonarProcessId(idProcesso);
            riga.setSonarControlType(TipoControllo.TIPO_METRICA);
            riga.setSonarKey(CHIAVE_COMPLESSITA_FUNZIONE);
            riga.setSonarValues(String.format(Locale.ROOT, "%.1f", complessita / funzioni));
            riga.setLanguage("Complexity");
            riga.setType("FLOAT");
            return riga;

        } catch (NullPointerException | NumberFormatException e) {
            return null;   // complexity o functions mancanti o non numerici
        }
    }

    // linee di codice per linguaggio: "java=1200;xml=80"
    public static List<SonarKpiReq> locPerLinguaggio(Long idProcesso, String distribuzione) {

        List<SonarKpiReq> righe = new ArrayList<>();

        if (distribuzione == null || distribuzione.isBlank()) {
            return righe;
        }

        for (String coppia : distribuzione.split(";")) {

            String[] parti = coppia.split("=");
            if (parti.length != 2 || parti[0].isBlank() || parti[1].isBlank()) {
                continue;
            }

            SonarKpiReq riga = new SonarKpiReq();
            riga.setSonarProcessId(idProcesso);
            riga.setSonarControlType(TipoControllo.TIPO_METRICA);
            riga.setSonarKey(CHIAVE_LOC_LINGUAGGIO);
            riga.setSonarValues(parti[1].trim());
            riga.setLanguage(parti[0].trim());     // chiave del linguaggio Sonar (java, js, xml...)
            riga.setType("INT");
            righe.add(riga);
        }
        return righe;
    }
}
