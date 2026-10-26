package com.soldati.dto.input;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class SonarKpiReq {
		
	private String sonarControlType;
	private String sonarKey;
	private Long sonarProcessId;
	private String sonarValues;
	private String severity;
	private String language;
	private String type;
}
