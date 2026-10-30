package com.soldati.regole;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.soldati.dto.output.KpiConfigDTO;

// regole del piano qualità: confronto con le soglie, tipo di inserimento, codice ISO
public final class ComplianceCalculator {

    public static final String ENTRO_SOGLIA = "Entro soglia";
    public static final String FUORI_SOGLIA = "Fuori soglia";
    public static final String SENZA_SOGLIE = "NP";
    public static final String NON_CONCORRE = "Non calcolare";

    public static final String INSERIMENTO_MANUALE = "Manuali";
    public static final String INSERIMENTO_AUTOMATICO = "Automatico";

    private static final Pattern NUMERO = Pattern.compile("^-?[0-9]+([.,][0-9]+)?$");

    private ComplianceCalculator() {
    }

    // sonar_values -> numero (per il confronto con le soglie)
    public static BigDecimal valoreNumerico(String valore) {

        if (valore == null) {
            return BigDecimal.ZERO;
        }

        String v = valore.trim();
        if (!NUMERO.matcher(v).matches()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v.replace(',', '.'));
    }

    // esito del confronto con le soglie
    public static String esitoSoglia(String sonarValues, KpiConfigDTO kpi) {

        if (kpi == null || !kpi.isCalcCompliance()) {
            return NON_CONCORRE;
        }

        BigDecimal min = kpi.getSogliaMin();
        BigDecimal max = kpi.getSogliaMax();

        if (min == null && max == null) {
            return SENZA_SOGLIE;
        }
        BigDecimal valore = valoreNumerico(sonarValues);
        boolean sopraMin = min == null || valore.compareTo(min) >= 0;
        boolean sottoMax = max == null || valore.compareTo(max) <= 0;

        return sopraMin && sottoMax ? ENTRO_SOGLIA : FUORI_SOGLIA;
    }

    // descrizione della riga della tabella tipo_inserimento
    public static String tipoInserimento(KpiConfigDTO compliance) {
        return compliance != null ? INSERIMENTO_MANUALE : INSERIMENTO_AUTOMATICO;
    }

    public static String codiceIso(KpiConfigDTO compliance, KpiConfigDTO others) {
        return compliance != null ? compliance.getIsoCode() : others.getIsoCode();
    }
}
