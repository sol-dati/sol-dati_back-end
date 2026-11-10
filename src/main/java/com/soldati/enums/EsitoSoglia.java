package com.soldati.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

// esito del confronto di un valore con le soglie
@Getter
@AllArgsConstructor
public enum EsitoSoglia {

    FUORI(0),
    ENTRO(1),
    NON_CONCORRE(998),
    NP(999);

    private final int idSoglia;

}