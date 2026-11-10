package com.soldati.dto.output;

import java.math.BigDecimal;

import com.soldati.enums.EsitoSoglia;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// kpi con severità blocker/critical nell'ultima analisi del tag
@Setter
@Getter
@Builder
public class IssueKpiDTO {

    private String isoCodice;
    private String isoDescrizione;
    private String chiave;
    private String nome;
    private String valore;
    private BigDecimal sogliaMin;
    private BigDecimal sogliaMax;
    private boolean calcCompliance;
    private EsitoSoglia esito;
}