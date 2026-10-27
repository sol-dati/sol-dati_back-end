package com.soldati.dto.output;

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
}
