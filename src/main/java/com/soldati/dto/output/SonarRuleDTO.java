package com.soldati.dto.output;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class SonarRuleDTO {
	
	private String ruleKey;
	private String ruleName;
	private String type;
	private String severity;
	private String language;
	private String description;
	private long issuesCount;

	// tag della regola e softwareQuality degli impacts: servono per il codice ISO 25010
	private List<String> tags;
	private List<String> softwareQualities;
}
