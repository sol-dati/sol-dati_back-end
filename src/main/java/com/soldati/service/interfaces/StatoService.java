package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.output.StatoDTO;

public interface StatoService {

    List<StatoDTO> findAll() throws Exception;
}
