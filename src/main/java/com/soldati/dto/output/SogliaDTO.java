package com.soldati.dto.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SogliaDTO {

    private Integer idSoglia;
    private String descrizione;
}
