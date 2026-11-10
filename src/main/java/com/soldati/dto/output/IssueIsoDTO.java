package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// kpi violati di una categoria ISO per una severità
@Setter
@Getter
@Builder
public class IssueIsoDTO {

    private String codice;
    private String descrizione;
    private long violati;
    private BigDecimal quota;
}