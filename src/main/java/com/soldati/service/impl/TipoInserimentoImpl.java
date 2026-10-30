package com.soldati.service.impl;

import org.springframework.stereotype.Service;

import com.soldati.dto.input.TipoInserimentoReq;
import com.soldati.dto.output.TipoInserimentoDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.Mapper;
import com.soldati.repository.TipoInserimentoRepo;
import com.soldati.service.interfaces.TipoInserimentoService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TipoInserimentoImpl implements TipoInserimentoService {

    private final TipoInserimentoRepo repo;

    @Override
    public TipoInserimentoDTO findByDescrizione(TipoInserimentoReq req) {

        return repo.findByDescrizione(req.getDescrizione())
                .map(Mapper::buildTipoInserimentoDTO)
                .orElseThrow(() -> new SoldatiException("Riga mancante in tipo_inserimento: '" + req.getDescrizione() + "'"));
    }
}
