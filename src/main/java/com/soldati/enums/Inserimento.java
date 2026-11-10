package com.soldati.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// tipo di inserimento del valore
@Getter
@AllArgsConstructor
public enum Inserimento {

    AUTOMATICO("A"),
    MANUALE("M");

    private final String idTipo;
}