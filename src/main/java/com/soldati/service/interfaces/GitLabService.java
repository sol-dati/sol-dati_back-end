package com.soldati.service.interfaces;

import java.io.File;
import java.util.List;

import com.soldati.dto.output.GitLabProjectDTO;
import com.soldati.exception.SoldatiException;

public interface GitLabService {

    // Recupera i nomi di tutti i progetti presenti su GitLab
    List<String> getProjectNames() throws SoldatiException;

    // Recupera i tag di un progetto presente su GitLab
    List<String> getTags(String projectName) throws SoldatiException;

    // Cerca un progetto per nome
    GitLabProjectDTO findProjectByName(String projectName) throws SoldatiException;

    // Scarica ed estrae i sorgenti di un tag specifico di un progetto
    File fetchSourceForTag(Long projectId, String tag) throws Exception;
}
