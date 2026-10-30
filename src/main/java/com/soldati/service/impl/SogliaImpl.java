package com.soldati.service.impl;

import org.springframework.stereotype.Service;

import com.soldati.dto.input.SogliaReq;
import com.soldati.dto.output.SogliaDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.Mapper;
import com.soldati.repository.SogliaRepo;
import com.soldati.service.interfaces.SogliaService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class SogliaImpl implements SogliaService {

    private final SogliaRepo repo;

    @Override
    public SogliaDTO findByDescrizione(SogliaReq req) {

        return repo.findByDescrizione(req.getDescrizione())
                .map(Mapper::buildSogliaDTO)
                .orElseThrow(() -> new SoldatiException("Riga mancante in soglie: '" + req.getDescrizione() + "'"));
    }
}
