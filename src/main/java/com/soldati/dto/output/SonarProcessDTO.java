package com.soldati.dto.output;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SonarProcessDTO {

    private Long id;
    private String projectName;
    private Long projectRelease;
    private Long sprint;
    private LocalDateTime processDate;
}
