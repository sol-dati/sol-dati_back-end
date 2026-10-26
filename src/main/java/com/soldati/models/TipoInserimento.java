package com.soldati.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tipo_inserimento")
public class TipoInserimento {

    @Id
    @Column(name = "id_tipo", length = 2, nullable = false)
    private String idTipo;

    @Column(name = "descrizione", length = 50, nullable = false)
    private String descrizione;
}