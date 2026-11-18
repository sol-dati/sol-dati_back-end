package com.soldati.regole;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.enums.EsitoSoglia;
import com.soldati.enums.Inserimento;
import com.soldati.models.SonarFact;

import lombok.NoArgsConstructor;

// regole del piano qualità: confronto con le soglie, tipo di inserimento, codice ISO, compliance
@NoArgsConstructor
public final class ComplianceCalculator {

    // compliance del piano qualità media sulle 8 categorie della iso 25010 (Extra iso NO)
    public static final int CARATTERISTICHE_ISO_25010 = 8;
    public static final String CODICE_ISO_EXTRA = "EX";

    private static final Pattern NUMERO = Pattern.compile("^-?[0-9]+([.,][0-9]+)?$");

    // conteggio / divisore (kpi entro soglia / kpi che concorrono alla compliance), 4 decimali; divisore 0 -> 0
    public static BigDecimal quota(long conteggio, long divisore) {
        if (divisore == 0) { 
            return BigDecimal.ZERO; 
        }
        return BigDecimal.valueOf(conteggio).divide(BigDecimal.valueOf(divisore), 4, RoundingMode.HALF_UP);
    }
 
    // kpi del catalogo che concorrono alla compliance in una categoria ISO
    public static long kpiCompliance(Map<String, KpiConfigDTO> catalogo, String isoCode) {
        return catalogo.values().stream()
                .filter(k -> isoCode.equals(k.getIsoCode()) && k.isCalcCompliance())
                .count();
    }

    // righe della categoria ISO che concorrono alla compliance con l'esito indicato
    public static long contaEsito(List<SonarFact> righe, Map<String, KpiConfigDTO> catalogo, String isoCode, EsitoSoglia esito) {
        return righe.stream()
                .filter(f -> Integer.valueOf(1).equals(f.getCalcCompliance()))
                .filter(f -> { 
                	KpiConfigDTO k = catalogo.get(f.getFkKpiCompliance());
                    return k != null && isoCode.equals(k.getIsoCode());
                })
                .filter(f -> Integer.valueOf(esito.getIdSoglia()).equals(f.getFkSoglia()))
                .count(); 
    } 

    // compliance del piano qualità: somma di KPI entro soglia / KPI che concorrono diviso le 8 categorie ISO 25010
    public static BigDecimal compliance(List<SonarFact> righe, Map<String, KpiConfigDTO> catalogo, List<String> categorie) {
        BigDecimal somma = BigDecimal.ZERO;
        for (String isoCode : categorie) {
            somma = somma.add(quota(contaEsito(righe, catalogo, isoCode, EsitoSoglia.ENTRO),
                    kpiCompliance(catalogo, isoCode)));
        }
        return somma.divide(BigDecimal.valueOf(CARATTERISTICHE_ISO_25010), 4, RoundingMode.HALF_UP);
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

    // true se il testo e' un numero (anche con la virgola decimale)
    public static boolean numeroValido(String valore) {
        return valore != null && NUMERO.matcher(valore.trim()).matches();
    }

    // i kpi che non concorrono alla compliance non vengono confrontati
    public static EsitoSoglia esitoSoglia(String sonarValues, KpiConfigDTO kpi) {
        if (kpi == null || !kpi.isCalcCompliance()) {
            return EsitoSoglia.NON_CONCORRE;
        }
        return confrontoSoglie(sonarValues, kpi.getSogliaMin(), kpi.getSogliaMax());
    }

    // confronto del valore con le soglie
    public static EsitoSoglia confrontoSoglie(String sonarValues, BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return EsitoSoglia.NP;
        }
        BigDecimal valore = valoreNumerico(sonarValues);
        boolean sopraMin = min == null || valore.compareTo(min) >= 0;
        boolean sottoMax = max == null || valore.compareTo(max) <= 0;
        return sopraMin && sottoMax ? EsitoSoglia.ENTRO : EsitoSoglia.FUORI;
    } 
 
    // tipo di inserimento della riga 
    public static Inserimento tipoInserimento(KpiConfigDTO compliance) {
        return compliance != null ? Inserimento.MANUALE : Inserimento.AUTOMATICO;
    }
 
    public static String codiceIso(KpiConfigDTO compliance, KpiConfigDTO others) {
        return compliance != null ? compliance.getIsoCode() : others.getIsoCode();
    }
}