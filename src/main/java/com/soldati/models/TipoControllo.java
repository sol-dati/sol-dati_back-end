package com.soldati.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tipo_controllo")
public class TipoControllo {

    public static final String TIPO_METRICA = "M";
    public static final String TIPO_RULE = "R";
    public static final String TIPO_MANUALE = "V";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo")
    private String sonarControlType;

    @Column(name = "descrizione")
    private String descrizione;
}
