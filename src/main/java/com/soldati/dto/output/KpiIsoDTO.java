package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// kpi di una categoria iso con valore ed esito dell'ultima analisi del tag
@Setter
@Getter
@Builder
public class KpiIsoDTO {

    private String chiave;
    private String nome;
    private String tipoControllo;
    private BigDecimal sogliaMin;
    private BigDecimal sogliaMax;
    private boolean calcCompliance;
    private String valore;
    private String esito;
}