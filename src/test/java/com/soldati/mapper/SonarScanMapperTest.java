package com.soldati.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.SonarResultDTO;
import com.soldati.dto.output.SonarRuleDTO;

class SonarScanMapperTest {

    private static AvviaSqReq richiesta(String progetto, String tag, Long release) {
        AvviaSqReq r = new AvviaSqReq();
        r.setProjectName(progetto);
        r.setTag(tag);
        r.setRelease(release);
        return r;
    }
    
    private static KpiConfigDTO catalogo(String chiave) {
        return KpiConfigDTO.builder().chiave(chiave).build();
    }

    private static SonarRuleDTO violata(String chiave) {
        return SonarRuleDTO.builder().ruleKey(chiave).build();
    }
    
    private static Map<String, Object> issue(String regola) {
        Map<String, Object> m = new HashMap<>();
        m.put("rule", regola);
        return m;
    }
    
    
   private static Map<String, Object> misura(String metric, Object value) {
       Map<String, Object> m = new HashMap<>();
       m.put("metric", metric);
       m.put("value", value);
       return m;
   }


    @Test
    void richiestaCompletaAccettata() {
    	
    	AvviaSqReq req = richiesta("agrivalor-backend", "Deploy_Review_R11_S1", 11L);

    	assertThatCode(() -> SonarScanMapper.validaRichiesta(req)).doesNotThrowAnyException();
    }
    
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            // projectName,         tag,                    release, nel messaggio
            "null,                  Deploy_Review_R11_S1,   11,      projectName",
            "' ',					Deploy_Review_R11_S1,	11,		 projectName",
            
            "agrivalor-backend,     null,   				11,      tag",
            "agrivalor-backend,		' ',					11,		 tag",
            
            "agrivalor-backend, 	Deploy_Review_R11_S1,	null,	 release"	
    })
    void richiestaIncompletaRifiutata(String projectName, String tag, Long release, String messaggio) {
    
    	AvviaSqReq req = richiesta(projectName, tag, release);
    	
    	assertThatThrownBy(() -> SonarScanMapper.validaRichiesta(req))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(messaggio);
    }

    @Test
    void soloLeRegoleAttiveNonViolateVannoSalvateAZero() {

        List<KpiConfigDTO> catalogo = List.of(
                catalogo("java:S1"),     // attiva e violata
                catalogo("java:S2"),     // attiva e NON violata
                catalogo("java:S3"));    // NON attiva sul profilo Sonar

        List<SonarRuleDTO> violate = List.of(violata("java:S1"));

        Set<String> attive = Set.of("java:S1","java:S2");

        List<String> ris = SonarScanMapper.regoleSenzaViolazioni(catalogo, violate, attive);

        assertThat(ris).containsExactly("java:S2");
    }
    
 

    @Test
    void contaLeIssuePerRegola() {

        List<Map<String, Object>> issues = List.of(
                issue("java:S1"),
                issue("java:S1"),
                issue("java:S2"),
                issue(null)
        );

        Map<String, Long> ris = SonarScanMapper.contaViolazioniPerRegola(issues);

        assertThat(ris).containsEntry("java:S1", 2L);
        assertThat(ris).containsEntry("java:S2", 1L);
        
        assertThat(ris).hasSize(2);
    }
    
    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            // sprint, chiave attesa
            "2,    agrivalor-backend-R11-S2",
            "null, agrivalor-backend-R11"
    })
    void chiaveProgettoConESenzaSprint(Long sprint, String attesa) {
        AvviaSqReq req = richiesta("agrivalor-backend", "Deploy_Review_R11_S1", 11L);
        req.setSprint(sprint);

        assertThat(SonarScanMapper.buildProjectKey(req)).isEqualTo(attesa);
    } 
    
    @Test
    void rispostaSenzaComponentRifiutata() {

        Map<String, Object> resultSonar = Map.of();

        assertThatThrownBy(() -> SonarScanMapper.buildSonarResult("agrivalor-backend-R11", resultSonar, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("agrivalor-backend-R11");
    }
  
    @Test
    void misuraConValoreSalvata() {
        Map<String, Object> resultSonar = Map.of(
                "component", Map.of(
                        "measures", List.of(misura("coverage", "81.2"))));
        
        SonarResultDTO ris = SonarScanMapper.buildSonarResult("agrivalor-backend-R11", resultSonar, List.of());
       
        assertThat(ris.getMetrics()).containsEntry("coverage", "81.2");
    }
    
    @Test
    void misuraSenzaValoreLettaDalPeriodo() {

        // 1. la misura: niente value, il valore sta dentro period
        Map<String, Object> conPeriodo = misura("new_coverage", null);
        conPeriodo.put("period", Map.of("value", 70));

        Map<String, Object> resultSonar = Map.of(
                "component", Map.of(
                        "measures", List.of(conPeriodo)));
        
        SonarResultDTO ris = SonarScanMapper.buildSonarResult("agrivalor-backend-R11", resultSonar, List.of());

        assertThat(ris.getMetrics()).containsEntry("new_coverage", "70");
    }
    
    @Test
    void misuraSenzaValoreLettaDalPrimoPeriodo() {

        Map<String, Object> conPeriodi = misura("new_bugs", null);
        conPeriodi.put("periods", List.of(Map.of("value", "3")));

        Map<String, Object> resultSonar = Map.of(
                "component", Map.of(
                        "measures", List.of(conPeriodi)));
        
        SonarResultDTO ris = SonarScanMapper.buildSonarResult("agrivalor-backend-R11", resultSonar, List.of());
        assertThat(ris.getMetrics()).containsEntry("new_bugs", "3");
    }
    
    @Test
    void misureIncompleteScartate() {

        Map<String, Object> resultSonar = Map.of(
                "component", Map.of(
                        "measures", List.of(
                                misura("coverage", "81.2"),
                                misura("duplicated_lines", null),
                                misura(null, "5"),
                                "non sono una mappa"
                        )));

        SonarResultDTO ris = SonarScanMapper.buildSonarResult("agrivalor-backend-R11", resultSonar, List.of());

        assertThat(ris.getMetrics())
                .containsEntry("coverage", "81.2")
                .hasSize(1);
    }
    
    @Test
    void regolaCostruitaDaiDettagliDiSonar() {
        Map<String, Object> dettagli = Map.of(
                "name", "Methods should not be empty",
                "type", "CODE_SMELL",
                "severity", "blocker",
                "htmlDesc", "<p>Empty methods...</p>",
                "lang", "java"
        );
        SonarRuleDTO ris = SonarScanMapper.buildRule("java:S1186", dettagli, 4L);

        assertThat(ris.getRuleKey()).isEqualTo("java:S1186");
        assertThat(ris.getRuleName()).isEqualTo("Methods should not be empty");
        assertThat(ris.getType()).isEqualTo("CODE_SMELL");
        assertThat(ris.getSeverity()).isEqualTo("blocker");
        assertThat(ris.getDescription()).isEqualTo("<p>Empty methods...</p>");
        assertThat(ris.getLanguage()).isEqualTo("java");
        assertThat(ris.getIssuesCount()).isEqualTo(4L);
    }
    
    @Test
    void processoCostruitoDallaRichiesta() {
        AvviaSqReq req = richiesta("agrivalor-backend", "Deploy_Review_R11_S1", 11L);
        req.setSprint(1L);

        LocalDateTime atteso = LocalDateTime.of(2026, 11, 13, 10, 30);
        Date momentoAvvio = Date.from(atteso.atZone(ZoneId.systemDefault()).toInstant());

        SonarProcessReq ris = SonarScanMapper.buildSonarProcessReq(req, momentoAvvio);
        
        assertThat(ris.getProjectName()).isEqualTo("agrivalor-backend");
        assertThat(ris.getProjectRelease()).isEqualTo(11L);
        assertThat(ris.getSprint()).isEqualTo(1L);
        assertThat(ris.getProcessDate()).isEqualTo(atteso);    
    } 
}