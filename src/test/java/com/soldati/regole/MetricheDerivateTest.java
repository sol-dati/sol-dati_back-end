package com.soldati.regole;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.soldati.dto.input.SonarKpiReq;

class MetricheDerivateTest {

	@Test
	void seSonarLaFornisceNonLaRicalcola() {
		
	    Map<String, String> metriche = Map.of(
	            MetricheDerivate.CHIAVE_COMPLESSITA_FUNZIONE, "2.7",   
	            "complexity", "30",                                    
	            "functions", "8"); 
	
	    assertThat(MetricheDerivate.complessitaPerFunzione(1L, metriche)).isNull();
		
	}
	
	@Test
	void complessitaDivisoFunzioniConUnDecimale() {
		
		Map<String, String> metriche = Map.of(   
	            "complexity", "30",                                    
	            "functions", "8");
		SonarKpiReq riga = MetricheDerivate.complessitaPerFunzione(1L, metriche);
		
		assertThat(riga.getSonarValues()).isEqualTo("3.8");
		
		assertThat(riga.getSonarKey()).isEqualTo(MetricheDerivate.CHIAVE_COMPLESSITA_FUNZIONE);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
	        // complexity, functions
	        "30,   null",
	        "null, 8",
	        "30, 0",
	        "x, 8"
	})
	void datiMancantiONonValidiNessunaRiga(String complessita, String funzioni) {
	    Map<String, String> metriche = new HashMap<>();
	    metriche.put("complexity", complessita);
	    metriche.put("functions", funzioni);

	    assertThat(MetricheDerivate.complessitaPerFunzione(1L, metriche)).isNull();
	}
	
    @ParameterizedTest
    @NullAndEmptySource                 
    @ValueSource(strings = " ")
    void distribuzioneVuotaNessunaRiga(String distribuzione) {
    	assertThat(MetricheDerivate.locPerLinguaggio(7L, distribuzione)).isEmpty();
    }

    @Test
    void unaRigaPerLinguaggio() {
        List<SonarKpiReq> righe = MetricheDerivate.locPerLinguaggio(7L, "java=1200;xml=80");

        assertThat(righe).extracting("language");
        
        assertThat(righe).extracting("sonarValues");
        
        assertThat(righe).allSatisfy(r -> {
            assertThat(r.getSonarKey()).isEqualTo(MetricheDerivate.CHIAVE_LOC_LINGUAGGIO);
            assertThat(r.getSonarProcessId()).isEqualByComparingTo(7L);
        });
    }

    @Test
    void coppieMalformateScartate() {
        List<SonarKpiReq> righe = MetricheDerivate.locPerLinguaggio(1L, "java=1200;=5;css=;xml= ;a=b=c");

        assertThat(righe).extracting("language").containsExactly("java");
    }

    @Test
    void linguaggioNonRiconosciutoTenuto() {
        List<SonarKpiReq> righe = MetricheDerivate.locPerLinguaggio(1L, "java=10;<null>=47");

        assertThat(righe).extracting("language").containsExactly("java", "<null>");
    }
}