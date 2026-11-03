package com.soldati.service.interfaces;

import java.util.List;

import com.soldati.dto.input.KpiManualiReq;
import com.soldati.dto.output.KpiManualeDTO;

public interface KpiManualiService {

	// KPI manuali del catalogo con i valori già inseriti nell'ultima analisi del tag
	List<KpiManualeDTO> elenco(String projectName, String tag);

	// salva i valori sull'ultima analisi del tag (sostituisce quelli precedenti)
	void salva(KpiManualiReq req);

	// dopo una nuova analisi Sonar ricopia i valori manuali dell'analisi precedente dello stesso tag
	void ricopiaSuNuovaAnalisi(String projectName, String tag, Long processoNuovo);
}
