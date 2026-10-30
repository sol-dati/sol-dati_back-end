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
@Table(name = "soglie")
public class Soglia {

    @Id
    @Column(name = "id_soglia", nullable = false)
    private Integer idSoglia;

    @Column(name = "descrizione", length = 50, nullable = false)
    private String descrizione;
}
