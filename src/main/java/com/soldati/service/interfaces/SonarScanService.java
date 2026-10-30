package com.soldati.service.interfaces;

import com.soldati.dto.input.AvviaSqReq;
import com.soldati.dto.output.SonarResultDTO;

public interface SonarScanService {

    // GitLab -> Maven -> attesa Sonar
    SonarResultDTO avviaAnalisi(AvviaSqReq req) throws Exception;
}
