package com.soldati.dto.output;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SonarResultDTO {

    private Map<String, String> metrics;

    private List<SonarRuleDTO> rules;

    private List<IsoCodeDTO> iso;
}
