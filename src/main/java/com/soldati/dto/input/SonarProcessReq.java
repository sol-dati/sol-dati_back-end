package com.soldati.dto.input;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SonarProcessReq {

    private String projectName;
    private Long projectRelease;
    private Long sprint;
    private LocalDateTime processDate;
}
