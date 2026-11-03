package com.soldati.dto.input;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class KpiManualiReq {

	private String projectName;
	private String tag;
	private List<KpiManualeReq> valori = new ArrayList<>();
}
