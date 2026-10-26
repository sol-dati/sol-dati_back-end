package com.soldati.dto.output;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class TipoInserimentoDTO {
	
	private String idTipo;
	private String descrizione;
}
