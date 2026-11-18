package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.output.ProjectDTO;
import com.soldati.enums.StatoAnalisi;
import com.soldati.exception.SoldatiException;

public interface ProjectService {

    int sincronizzaDaGitLab() throws SoldatiException;
    List<ProjectDTO> findAll(String projectName) throws SoldatiException;
    List<ProjectDTO> findAnalizzati();
    void aggiornaStato(String projectName, String tag, StatoAnalisi stato) throws SoldatiException;

    // tag rimasti in corso da un'esecuzione interrotta -> errore
    int chiudiAnalisiInterrotte() throws SoldatiException;
    
    //eccezione se un'analisi dello stesso tag è già in corso
    void avviaElaborazione(String projectName, String tag) throws SoldatiException;
}
