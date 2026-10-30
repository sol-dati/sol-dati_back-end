package com.soldati.dto.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StatoDTO {

    private Long statoId;
    private String descrizione;
}
