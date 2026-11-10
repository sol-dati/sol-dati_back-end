package com.soldati.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// tipo di kpi del catalogo
@Getter
@AllArgsConstructor
public enum TipoKpi {

    AUTOMATICO("A"),
    MANUALE("M");

    private final String codice;
}