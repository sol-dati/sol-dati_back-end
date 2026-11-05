package com.soldati.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.soldati.dto.output.StatoDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.Mapper;
import com.soldati.repository.StatoRepo;
import com.soldati.service.interfaces.StatoService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class StatoImpl implements StatoService {

    private final StatoRepo repo;

    @Override
    public List<StatoDTO> findAll() throws SoldatiException {

        return repo.findAll()
                .stream()
                .map(Mapper::buildStatoDTO)
                .toList();
    }
}
