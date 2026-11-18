package com.soldati.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.soldati.dto.output.DimensioneCodiceDTO;
import com.soldati.dto.output.IsoSintesiDTO;
import com.soldati.dto.output.IssueKpiDTO;
import com.soldati.dto.output.IssueSezioneDTO;
import com.soldati.dto.output.KpiConfigDTO;
import com.soldati.dto.output.KpiIsoDTO;
import com.soldati.enums.EsitoSoglia;
import com.soldati.enums.GruppoKpi;
import com.soldati.models.IsoCode;
import com.soldati.models.LanguageSonar;
import com.soldati.models.SonarFact;
import com.soldati.models.TipoControllo;

class DashboardMapperTest {

	private static KpiConfigDTO kpi(String chiave) {
	    return KpiConfigDTO.builder()
	    		.chiave(chiave)
	    		.build();
	}
	
	private static KpiConfigDTO kpi(String chiave, String min, String max) {
	    return KpiConfigDTO.builder()
	            .chiave(chiave)
	            .sogliaMin(min != null ? new BigDecimal(min) : null)
	            .sogliaMax(max != null ? new BigDecimal(max) : null)
	            .build();
	}
	
	private static SonarFact fact(String valore, String min, String max) {
	    TipoControllo tipo = new TipoControllo();
	    tipo.setDescrizione("Metrica");          

	    SonarFact f = new SonarFact();
	    f.setTipoControllo(tipo);
	    f.setSonarValues(valore);
	    f.setSogliaMin(min != null ? new BigDecimal(min) : null);
	    f.setSogliaMax(max != null ? new BigDecimal(max) : null);
	    return f;
	}
	
	private static SonarFact factL(String chiave, String valore, String linguaggio) {
		
		SonarFact f = new SonarFact();
		
	    f.setSonarKey(chiave);
	    f.setSonarValues(valore);
	    
	    if (linguaggio != null) {
	        LanguageSonar l = new LanguageSonar();
	        l.setName(linguaggio);
	        f.setLanguage(l);
	    }
	    
	    return f;
	}
	
	private static IssueKpiDTO issue(String iso, String descrizioneIso, String chiave, String valore) {
	    return IssueKpiDTO.builder()
	            .isoCodice(iso)
	            .isoDescrizione(descrizioneIso)
	            .chiave(chiave)
	            .valore(valore)
	            .build();
	}
	 
	@ParameterizedTest
	@CsvSource({
		// chiave,      gruppo atteso
        "java:S108,     REGOLA",
        "Web:UnclosedTagCheck,		REGOLA",
        "comment_lines_density,	METRICA",
        "CFCP,		METRICA" 
	})
	void gruppoDallaChiave(String chiave, GruppoKpi gruppo) {
		
		KpiIsoDTO ris = DashboardMapper.buildKpiIso(kpi(chiave), null);
	    assertThat(ris.getGruppo()).isEqualTo(gruppo);
	}
	
	@Test
	void kpiNonMisuratoSenzaValoreNeEsito() {

	    KpiConfigDTO kpi = kpi("java:S108", "0", "0");

	    KpiIsoDTO ris = DashboardMapper.buildKpiIso(kpi, null);

	    assertThat(ris.getValore()).isNull();
	    assertThat(ris.getEsito()).isNull();
	    assertThat(ris.getTipoControllo()).isNull();
	    assertThat(ris.getSogliaMax()).isZero();
	}
	
	@Test
	void sogliaDellAnalisiHaPrecedenzaSulCatalogo() {

	    KpiConfigDTO kpi = kpi("duplicated_lines_density", null, "10");
	    SonarFact riga = fact("7", null, "5");

	    KpiIsoDTO ris = DashboardMapper.buildKpiIso(kpi, riga);

	    assertThat(ris.getSogliaMax()).isEqualByComparingTo("5");
	    assertThat(ris.getEsito()).isEqualTo(EsitoSoglia.FUORI);
	}
	
	@Test
	void senzaSogliaNellAnalisiUsaQuellaDelCatalogo() {

	    KpiConfigDTO kpi = kpi("duplicated_lines_density", null, "10");
	    SonarFact riga = fact("7", null, null);

	    KpiIsoDTO ris = DashboardMapper.buildKpiIso(kpi, riga);

	    assertThat(ris.getSogliaMax()).isEqualByComparingTo("10");
	    assertThat(ris.getEsito()).isEqualTo(EsitoSoglia.ENTRO);
	}
    
	@Test
	void sintesiCalcolaLeQuoteEIlResto() {

	    IsoCode iso = new IsoCode();
	    iso.setIso_code("M");
	    iso.setDescription("Manutenibilita");

	    IsoSintesiDTO ris = DashboardMapper.buildIsoSintesi(iso, 10, 6, 3);

	    assertThat(ris.getEntro()).isEqualByComparingTo("0.6000");
	    assertThat(ris.getFuori()).isEqualByComparingTo("0.3000");
	    assertThat(ris.getNp()).isEqualByComparingTo("0.1000");
	}
	
	@Test
	void issueContaIViolatiPerCategoriaDalPiuViolato() {

	    List<IssueKpiDTO> kpi = List.of(
	            issue("M", "Manutenibilità", "java:S1", "3"),
	            issue("M", "Manutenibilità", "java:S2", "0"),
	            issue("M", "Manutenibilità", "java:S5", "1"),
	            issue("M", "Manutenibilità", "java:S6", "2"),
	            issue("S", "Sicurezza", "java:S3", "1"),
	            issue("S", "Sicurezza", "java:S4", "0")
	    );

	    IssueSezioneDTO ris = DashboardMapper.buildIssueSezione("CRITICAL", kpi);

	    assertThat(ris.getTotaleViolati()).isEqualTo(4L);
	    
	    assertThat(ris.getViolatiPerIso()).extracting("codice").containsExactly("M", "S");
	    
	    assertThat(ris.getViolatiPerIso()).extracting("violati").containsExactly(3L, 1L);
	    
	    assertThat(ris.getViolatiPerIso().get(0).getQuota()).isEqualByComparingTo("0.75");
	    
	    assertThat(ris.getKpi()).hasSize(6);
	} 

	@Test
	void dimensioneSommaLeRighePerLinguaggioDalPiuGrande() {
	    
		List<SonarFact> kpi = List.of(
				factL("files", "12", null),
				factL("ncloc_language", "200", "java"),
				factL("ncloc_language", "100", "xml"),
				factL("ncloc_language", "100", "xml"),
				factL("ncloc_language", "50", null)
				);
		
		DimensioneCodiceDTO ris = DashboardMapper.buildDimensioneCodice(kpi);
	    
		assertThat(ris.getNumeroFile()).isEqualTo(12L);
	    
		assertThat(ris.getRigheCodice()).isEqualTo(450L);
		
		assertThat(ris.getLinguaggi()).extracting("linguaggio").containsExactly("java", "xml", "Altro");
		
		assertThat(ris.getLinguaggi()).extracting("righe").containsExactly(200L, 200L, 50L);
		
		assertThat(ris.getLinguaggi().get(0).getQuota()).isEqualByComparingTo("0.4444");
	}
	
	@Test
	void issueKpiPrendeDescrizioneCategoriaEdEsito() {
		
		KpiConfigDTO kpi = KpiConfigDTO.builder()
	            .chiave("java:S1")
	            .isoCode("M")
	            .sogliaMax(new BigDecimal("10"))
	            .build();
		
		SonarFact riga = fact("11", null, null);
		
		Map<String, String> categorie = Map.of("M", "manutenibilita");
		
		IssueKpiDTO ris = DashboardMapper.buildIssueKpi(kpi, riga, categorie);

		assertThat(ris.getIsoDescrizione()).isEqualTo("manutenibilita");
		
		assertThat(ris.getValore()).isEqualTo("11");
		
	    assertThat(ris.getEsito()).isEqualTo(EsitoSoglia.FUORI);
	}

	@Test
	void issueKpiSenzaCategoriaUsaIlTrattino() {
		
		KpiConfigDTO kpi = KpiConfigDTO.builder()
	            .chiave("java:S1")
	            .isoCode("M")
	            .sogliaMax(new BigDecimal("10"))
	            .build();
		
		SonarFact riga = fact("11", null, null);
		
		Map<String, String> categorie = Map.of();
		
		IssueKpiDTO ris = DashboardMapper.buildIssueKpi(kpi, riga, categorie);
		
		assertThat(ris.getIsoDescrizione()).isEqualTo("-");
		
		assertThat(ris.getValore()).isEqualTo("11");
		
	    assertThat(ris.getEsito()).isEqualTo(EsitoSoglia.FUORI);
	}
}