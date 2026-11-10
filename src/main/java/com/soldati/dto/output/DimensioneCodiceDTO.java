package com.soldati.dto.output;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// dimensione del codice nell'ultima analisi del tag
@Setter
@Getter
@Builder
public class DimensioneCodiceDTO {

    private long numeroFile;
    private long righeCodice;
    private List<LinguaggioLocDTO> linguaggi;
}