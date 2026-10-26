package com.soldati.dto.output;

import java.time.LocalDate;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class ProjectDTO {

	private Long id;
	private String projectName;
	private Long releaseNumber;
	private Long sprintNumber;
	private String tag;
	private LocalDate dataInserimento;
	
	private Long statoId;
	private String descrizione;
}
