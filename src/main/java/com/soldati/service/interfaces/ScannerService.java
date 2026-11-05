package com.soldati.service.interfaces;

import java.io.File;

import com.soldati.exception.SoldatiException;

public interface ScannerService {

    // analisi maven + sonar sulla cartella indicata, projectKey passata per identificare l'analisi su SonarQube
	int runSonarAnalisi(File sourceDir, String projectName, String projectKey) throws SoldatiException;
}
