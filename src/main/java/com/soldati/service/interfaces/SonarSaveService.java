package com.soldati.service.interfaces;

import com.soldati.dto.input.SonarProcessReq;
import com.soldati.dto.output.SonarProcessDTO;
import com.soldati.dto.output.SonarResultDTO;

public interface SonarSaveService {

    SonarProcessDTO salvaAnalisi(SonarProcessReq req, String tag, SonarResultDTO result);

    void salvaFact(Long processId, String projectName, String tag, SonarResultDTO result);

    SonarResultDTO getUltimoRisultato(Long projectId);
}
