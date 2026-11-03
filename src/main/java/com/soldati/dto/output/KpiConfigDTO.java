package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class KpiConfigDTO {

    private String chiave;
    private String nome;
    private String isoCode;
    private BigDecimal sogliaMin;
    private BigDecimal sogliaMax;
    private boolean calcCompliance;
    private String tipoKpi;
    private String severita;
    private String descrizione;
}
