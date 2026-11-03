package com.soldati.dto.output;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    private LocalDateTime dataInvioManuali;
}
