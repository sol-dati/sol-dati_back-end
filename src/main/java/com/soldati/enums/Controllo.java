package com.soldati.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

//tipo controllo del valore
@Getter
@AllArgsConstructor
public enum Controllo {
	
	METRICA("M"),
    REGOLA("R"),
	MANUALE("V");

	private final String codice;
}
