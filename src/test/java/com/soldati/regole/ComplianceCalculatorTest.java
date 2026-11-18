package com.soldati.regole;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.enums.EsitoSoglia;
import com.soldati.enums.Inserimento;
import com.soldati.models.IsoCode;
import com.soldati.models.SonarFact;

class ComplianceCalculatorTest {

	private static final Map<String, KpiConfigDTO> CATALOGO = Map.of(
			"M1", kpi("M1", "M", true), 
			"M2", kpi("M2", "M", true), 
			"M3", kpi("M3", "M", false),
			"M4", kpi("M4", "M", false),
			"S1", kpi("S1", "S", true),
			"A1", kpi("A1", "A", true),
			"A2", kpi("A2", "A", true)
			);
	
	private static final List<SonarFact> RIGA = List.of(
			riga("M1", true, EsitoSoglia.ENTRO), 
			riga("M2", true, EsitoSoglia.FUORI), 
			riga("M3", false, EsitoSoglia.NON_CONCORRE), 
			riga("M4", false, EsitoSoglia.ENTRO),
			riga("S1", true, EsitoSoglia.ENTRO),
			riga("A2", true, EsitoSoglia.ENTRO)
			);
	
	private static KpiConfigDTO kpi(String chiave, String iso, boolean calc) {
		return KpiConfigDTO.builder()
				.chiave(chiave)
				.isoCode(iso)
				.calcCompliance(calc)
				.build();
	}
	
	private static SonarFact riga(String chiave, boolean calc, EsitoSoglia esito) {
	    SonarFact f = new SonarFact();
	    
	    f.setFkKpiCompliance(chiave);
	    f.setCalcCompliance(calc ? 1 : 0);
	    f.setFkSoglia(esito.getIdSoglia());
	    return f;
	}

	@Test
	void dividePerQuattro() {

	    BigDecimal ris = ComplianceCalculator.quota(1, 4);

	    assertThat(ris).isEqualByComparingTo("0.25");   
	    assertThat(ris.scale()).isEqualTo(4);            
	}

	@Test
	void dividePerZeroSenzaErrore() {

		BigDecimal ris = ComplianceCalculator.quota(5, 0);

		assertThat(ris).isEqualByComparingTo("0");
	}

	@Test
	void dueDivisoTreArrotondaPerEccesso() {

		BigDecimal ris = ComplianceCalculator.quota(2, 3);

		assertThat(ris).isEqualByComparingTo("0.6667");
	}

	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
			// valore, min, max, atteso 
			"4.5,      null, 	10,   ENTRO", 
			"17.1,	25,		null,	FUORI", 
			"10,	null,	10,	ENTRO",
			"1,	null,	null,	NP", 
			"12,	null,	10,	FUORI", 
			"10,	10,	null,	ENTRO" 
	})
	void confrontoSoglie(String valore, BigDecimal min, BigDecimal max, EsitoSoglia atteso) {

		EsitoSoglia ris = ComplianceCalculator.confrontoSoglie(valore, min, max);

		assertThat(ris).isEqualTo(atteso);
	}

	@Test
	void kpiComplianceContaSoloQuelliCheConcorrono() {

		long ris = ComplianceCalculator.kpiCompliance(CATALOGO, "M");

		assertThat(ris).isEqualTo(2L); 
		
		assertThat(ComplianceCalculator.kpiCompliance(CATALOGO, "U")).isZero();

	}
	
	@Test
	void kpiComplianceContaInBaseAllEsito() {
		assertThat(ComplianceCalculator.contaEsito(RIGA, CATALOGO, "M", EsitoSoglia.ENTRO)).isEqualTo(1L);
		assertThat(ComplianceCalculator.contaEsito(RIGA, CATALOGO, "M", EsitoSoglia.FUORI)).isEqualTo(1L);
		assertThat(ComplianceCalculator.contaEsito(RIGA, CATALOGO, "S", EsitoSoglia.ENTRO)).isEqualTo(1L);
	}
	
	@Test
	void calcoloComlianceKpiEntroPerCategoria() {
		assertThat(ComplianceCalculator.compliance(RIGA, CATALOGO, List.of("M", "S", "A"))).isEqualByComparingTo("0.2500");
	}

	
	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
			// valore, atteso 
			"'4,5', 4.5", 
			"null, 0", 
			"string, 0",
			"-5, -5"
	})
	void valoreNumericoDaStringANumero(String valore, String atteso) {
		assertThat(ComplianceCalculator.valoreNumerico(valore)).isEqualByComparingTo(atteso);
	}
	
	@ParameterizedTest
	@CsvSource(nullValues = "null", value = {
			// valore, atteso 
			"'4,5', true", 
			"null, false", 
			"string, false",
			"-5, true"
	})
	void numeroValidoRiconosceSoloINumeri(String valore, Boolean atteso) {
		assertThat(ComplianceCalculator.numeroValido(valore)).isEqualTo(atteso);
	}
	
	@Test
	void kpiAssenteNonConcorre() {
		assertThat(ComplianceCalculator.esitoSoglia("10", null)).isEqualTo(EsitoSoglia.NON_CONCORRE);
	}
	
	@Test
	void kpiConCalcFalseNonConcorreAncheSeEntroSoglia() {

		KpiConfigDTO kpi = KpiConfigDTO.builder()
	            .chiave("java:S1")
	            .isoCode("M")
				.calcCompliance(false)
				.sogliaMin(new BigDecimal("4"))
				.sogliaMax(new BigDecimal("15"))
				.build();
		
		assertThat(ComplianceCalculator.esitoSoglia("10", kpi)).isEqualTo(EsitoSoglia.NON_CONCORRE);
	}
	
	@Test
	void kpiCheConcorreVieneConfrontatoConLeSoglie() {

		KpiConfigDTO kpi = KpiConfigDTO.builder()
	            .chiave("java:S1")
	            .isoCode("M")
				.calcCompliance(true)
				.sogliaMin(new BigDecimal("4"))
				.sogliaMax(new BigDecimal("15"))
				.build();
		
		assertThat(ComplianceCalculator.esitoSoglia("10", kpi)).isEqualTo(EsitoSoglia.ENTRO);
	}
	
	@Test
	void complianceNullInserimentoAutomatico() {
		assertThat(ComplianceCalculator.tipoInserimento(null)).isEqualTo(Inserimento.AUTOMATICO);
	}
	
	@Test
	void complianceConcorrePresenteInserimentoManuale() {
		KpiConfigDTO compliance = KpiConfigDTO.builder()
				.calcCompliance(true)
				.build();
		
		assertThat(ComplianceCalculator.tipoInserimento(compliance)).isEqualTo(Inserimento.MANUALE);
	}
	
	@Test
	void complianceNonConcorrePresenteInserimentoManuale() {
		KpiConfigDTO compliance = KpiConfigDTO.builder()
				.calcCompliance(false)
				.build();
		
		assertThat(ComplianceCalculator.tipoInserimento(compliance)).isEqualTo(Inserimento.MANUALE);
	}
	
	@Test
	void codiceIsoPreferisceIlCatalogoCompliance() {
	    KpiConfigDTO compliance = KpiConfigDTO.builder().isoCode("M").build();
	    KpiConfigDTO others = KpiConfigDTO.builder().isoCode("S").build();

	    assertThat(ComplianceCalculator.codiceIso(compliance, others)).isEqualTo("M");
	}

	@Test
	void codiceIsoSenzaComplianceUsaOthers() {
	    KpiConfigDTO others = KpiConfigDTO.builder().isoCode("S").build();

	    assertThat(ComplianceCalculator.codiceIso(null, others)).isEqualTo("S");
	}
}