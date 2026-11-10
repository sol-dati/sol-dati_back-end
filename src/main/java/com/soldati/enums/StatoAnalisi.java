package com.soldati.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatoAnalisi {

    DA_ANALIZZARE(1L),
    IN_CORSO(10L),
    ELABORATO(2L),
    ERRORE(-5L);

    private final long idStato;
}