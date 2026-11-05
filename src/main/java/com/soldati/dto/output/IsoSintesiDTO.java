package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// compliance di una categoria iso nell'ultima analisi del tag: kpi entro soglia, fuori soglia e non presenti
@Setter
@Getter
@Builder
public class IsoSintesiDTO {

    private String codice;
    private String descrizione;
    private long kpiCompliance;
    private BigDecimal entro;
    private BigDecimal fuori;
    private BigDecimal np;
}