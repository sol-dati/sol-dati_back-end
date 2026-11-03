package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class KpiManualeDTO {

	private String chiave;
	private String nome;
	private String descrizione;
	private String isoCode;
	private BigDecimal sogliaMin;
	private BigDecimal sogliaMax;
	private boolean calcCompliance;
	private String valore;
}
