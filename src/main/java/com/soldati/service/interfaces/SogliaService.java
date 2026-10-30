package com.soldati.service.interfaces;

import com.soldati.dto.input.SogliaReq;
import com.soldati.dto.output.SogliaDTO;

public interface SogliaService {

    SogliaDTO findByDescrizione(SogliaReq req);
}
