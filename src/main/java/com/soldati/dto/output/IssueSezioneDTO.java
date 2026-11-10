package com.soldati.dto.output;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// grafico dei violati per categoria + tabella dei KPI
@Setter
@Getter
@Builder
public class IssueSezioneDTO {

    private String severita;
    private long totaleViolati;
    private List<IssueIsoDTO> violatiPerIso;
    private List<IssueKpiDTO> kpi;
}