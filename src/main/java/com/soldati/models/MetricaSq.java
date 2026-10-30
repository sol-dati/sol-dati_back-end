package com.soldati.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "metriche_sq", uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_metriche_sq_metrica",
            columnNames = "metrica"
        )
    }
)
public class MetricaSq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metrica", nullable = false, unique = true)
    private String metrica;
}
