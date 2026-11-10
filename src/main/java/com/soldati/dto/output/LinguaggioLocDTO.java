package com.soldati.dto.output;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// righe di codice di un linguaggio nell'ultima analisi del tag
@Setter
@Getter
@Builder
public class LinguaggioLocDTO {

    private String linguaggio;
    private long righe;
    private BigDecimal quota;
}