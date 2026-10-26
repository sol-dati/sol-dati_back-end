package com.soldati.dto.input;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class AvviaSqReq {
	
	private String projectName;
    private Long release;
    private Long sprint;
    private String tag;

}
