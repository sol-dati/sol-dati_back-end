package com.soldati.service.interfaces;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.soldati.exception.SoldatiException;

public interface SonarApiService {

    // verifica se SonarQube sta ancora elaborando l'analisi.
    boolean isRunning(String projectKey, Date startProcess) throws SoldatiException;

    // recupera le metriche principali
    Map<String, Object> getResult(String projectKey) throws SoldatiException;

    // recupera i dettagli (nome, descrizione, tipo ecc.)
    Map<String, Object> getRuleDetails(String ruleKey) throws SoldatiException;

    // lista dei problemi
    List<Map<String, Object>> getIssues(String projectKey) throws SoldatiException;

    // chiavi delle regole attive nei Quality Profile usati dal progetto (cioe' le regole che Sonar ha controllato)
    Set<String> getRegoleAttive(String projectKey) throws SoldatiException;

    // elenco delle metriche disponibili
    List<Map<String, Object>> getAvailableMetrics() throws SoldatiException;

    // elenco dei linguaggi
    List<Map<String, Object>> getAvailableLanguages() throws SoldatiException;

    // elenco dei tipi
    List<String> getAvailableTypes() throws SoldatiException;

    // elenco delle severity
    List<String> getAvailableSeverities() throws SoldatiException;
}
