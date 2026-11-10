package com.soldati.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.soldati.dto.output.DimensioneCodiceDTO;
import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.IssueIsoDTO;
import com.soldati.dto.output.IssueKpiDTO;
import com.soldati.dto.output.IssueSezioneDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiIsoDTO;
import com.soldati.dto.output.LinguaggioLocDTO;
import com.soldati.enums.GruppoKpi;
import com.soldati.models.IsoCode;
import com.soldati.models.LanguageSonar;
import com.soldati.models.SonarFact;
import com.soldati.regole.ComplianceCalculator;
import com.soldati.regole.MetricheDerivate;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class DashboardMapper {

    // chiave Sonar del numero di file
    private static final String CHIAVE_FILE = "files";
	
    // quote della categoria
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
    public static KpiIsoDTO buildKpiIso(KpiConfigDTO kpi, SonarFact fact) {

        if (fact == null) {
            return KpiIsoDTO.builder()
                    .chiave(kpi.getChiave())
                    .nome(kpi.getNome())
                    .descrizione(kpi.getDescrizione())
                    .gruppo(gruppo(kpi))
                    .sogliaMin(kpi.getSogliaMin())
                    .sogliaMax(kpi.getSogliaMax())
                    .calcCompliance(kpi.isCalcCompliance())
                    .build();
        }

        BigDecimal min = sogliaMin(kpi, fact);
        BigDecimal max = sogliaMax(kpi, fact);

        return KpiIsoDTO.builder()
                .chiave(kpi.getChiave())
                .nome(kpi.getNome())
                .descrizione(kpi.getDescrizione())
                .gruppo(gruppo(kpi))
                .tipoControllo(fact.getTipoControllo().getDescrizione())
                .sogliaMin(min)
                .sogliaMax(max)
                .calcCompliance(kpi.isCalcCompliance())
                .valore(fact.getSonarValues())
                .esito(ComplianceCalculator.confrontoSoglie(fact.getSonarValues(), min, max))
                .build();
    }

    // riga della tabella issue
    public static IssueKpiDTO buildIssueKpi(KpiConfigDTO kpi, SonarFact fact, Map<String, String> categorie) {
        BigDecimal min = sogliaMin(kpi, fact);
        BigDecimal max = sogliaMax(kpi, fact);
        return IssueKpiDTO.builder()
                .isoCodice(kpi.getIsoCode())
                .isoDescrizione(categorie.getOrDefault(kpi.getIsoCode(), "-"))
                .chiave(kpi.getChiave())
                .nome(kpi.getNome())
                .valore(fact.getSonarValues())
                .sogliaMin(min)
                .sogliaMax(max)
                .calcCompliance(kpi.isCalcCompliance())
                .esito(ComplianceCalculator.confrontoSoglie(fact.getSonarValues(), min, max))
                .build();
    }

    // blocco di una severità: kpi violati contati per categoria
    public static IssueSezioneDTO buildIssueSezione(String severita, List<IssueKpiDTO> kpi) {

        List<IssueKpiDTO> violati = kpi.stream()
                .filter(k -> ComplianceCalculator.valoreNumerico(k.getValore()).signum() > 0)
                .toList();

        Map<String, List<IssueKpiDTO>> perIso = violati.stream()
                .collect(Collectors.groupingBy(IssueKpiDTO::getIsoCodice));

        List<IssueIsoDTO> violatiPerIso = perIso.values().stream()
                .map(gruppo -> IssueIsoDTO.builder()
                        .codice(gruppo.get(0).getIsoCodice())
                        .descrizione(gruppo.get(0).getIsoDescrizione())
                        .violati(gruppo.size())
                        .quota(ComplianceCalculator.quota(gruppo.size(), violati.size()))
                        .build())
                .sorted(Comparator.comparingLong(IssueIsoDTO::getViolati).reversed()
                        .thenComparing(IssueIsoDTO::getDescrizione))
                .toList();

        return IssueSezioneDTO.builder()
                .severita(severita)
                .totaleViolati(violati.size())
                .violatiPerIso(violatiPerIso)
                .kpi(kpi)
                .build();
    }
    
    // file e righe di codice dell'ultima analisi
    public static DimensioneCodiceDTO buildDimensioneCodice(List<SonarFact> righe) {

        long numeroFile = righe.stream()
                .filter(f -> CHIAVE_FILE.equals(f.getSonarKey()))
                .findFirst()
                .map(f -> ComplianceCalculator.valoreNumerico(f.getSonarValues()).longValue())
                .orElse(0L);

        Map<String, Long> righePerLinguaggio = righe.stream()
                .filter(f -> MetricheDerivate.CHIAVE_LOC_LINGUAGGIO.equals(f.getSonarKey()))
                .collect(Collectors.groupingBy(DashboardMapper::nomeLinguaggio,
                        Collectors.summingLong(f -> ComplianceCalculator.valoreNumerico(f.getSonarValues()).longValue())));

        long totale = righePerLinguaggio.values().stream().mapToLong(Long::longValue).sum();

        List<LinguaggioLocDTO> linguaggi = righePerLinguaggio.entrySet().stream()
                .map(e -> LinguaggioLocDTO.builder()
                        .linguaggio(e.getKey())
                        .righe(e.getValue())
                        .quota(ComplianceCalculator.quota(e.getValue(), totale))
                        .build())
                .sorted(Comparator.comparingLong(LinguaggioLocDTO::getRighe).reversed()
                        .thenComparing(LinguaggioLocDTO::getLinguaggio))
                .toList();

        return DimensioneCodiceDTO.builder()
                .numeroFile(numeroFile)
                .righeCodice(totale)
                .linguaggi(linguaggi)
                .build();
    }

    // soglie salvate con l'analisi; se mancano, quelle del catalogo
    private static BigDecimal sogliaMin(KpiConfigDTO kpi, SonarFact fact) {
        return fact.getSogliaMin() != null ? fact.getSogliaMin() : kpi.getSogliaMin();
    }

    private static BigDecimal sogliaMax(KpiConfigDTO kpi, SonarFact fact) {
        return fact.getSogliaMax() != null ? fact.getSogliaMax() : kpi.getSogliaMax();
    }
    
    // le regole contengono ":", metriche e KPI manuali no
    private static GruppoKpi gruppo(KpiConfigDTO kpi) {
        return kpi.getChiave() != null && kpi.getChiave().contains(":") ? GruppoKpi.REGOLA : GruppoKpi.METRICA;
    }
    
    // nome leggibile del linguaggio, altrimenti la chiave Sonar
    private static String nomeLinguaggio(SonarFact f) {
        LanguageSonar l = f.getLanguage();
        if (l == null) {
            return "Altro";
        }
        return l.getName() != null ? l.getName() : l.getLanguage();
    }
}