package com.soldati.service.interfaces;

import java.util.List;
import java.util.Map;

import com.soldati.dto.input.SonarKpiReq;
import com.soldati.exception.SoldatiException;
import com.soldati.models.LanguageSonar;

// allinea le tabelle di catalogo (metriche_sq, language, type, severity) con quanto letto da SonarQube
public interface CatalogoSonarService {

    // inserisce le metriche nuove, ritorna il numero di nuovi inserimenti
    int sincronizzaMetriche() throws SoldatiException;

    // inserisce/aggiorna, ritorna il numero di nuovi inserimenti
    int sincronizzaLanguage() throws SoldatiException;

    int sincronizzaType() throws SoldatiException;

    int sincronizzaSeverity() throws SoldatiException;

    // language per chiave; quelli delle righe metrica non ancora presenti vengono inseriti
    Map<String, LanguageSonar> leggiLanguage(List<SonarKpiReq> righe);
}
