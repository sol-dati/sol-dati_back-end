package com.soldati.service.interfaces;

import com.soldati.dto.input.TipoInserimentoReq;
import com.soldati.dto.output.TipoInserimentoDTO;

public interface TipoInserimentoService {

	// riga di dim_tipo_inserimento con la descrizione richiesta
	TipoInserimentoDTO findByDescrizione(TipoInserimentoReq req);
}
