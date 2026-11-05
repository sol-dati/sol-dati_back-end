package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.output.ProjectDTO;
import com.soldati.exception.SoldatiException;

public interface ProjectService {

    int sincronizzaDaGitLab() throws SoldatiException;
    List<ProjectDTO> findAll(String projectName) throws SoldatiException;
    List<ProjectDTO> findAnalizzati();
}
