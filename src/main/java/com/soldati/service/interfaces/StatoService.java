package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.output.StatoDTO;
import com.soldati.exception.SoldatiException;

public interface StatoService {

    List<StatoDTO> findAll() throws SoldatiException;
}
