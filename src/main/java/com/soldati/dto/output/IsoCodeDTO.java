package com.soldati.dto.output;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class IsoCodeDTO {

	private String isoCode;
    private String descrizione;
    private Map<String, String> metrics;
    private List<SonarRuleDTO> rules;
}
