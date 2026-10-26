package com.soldati.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sonar_kpi")
public class SonarKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sonar_process_id", nullable = false)
    private Long sonarProcessId;

    @Column(name = "sonar_key")
    private String sonarKey;

    @Column(name = "severity")
    private String severity;

    @Column(name = "language")
    private String language;
    
    @Column(name = "type")
    private String type;

    @Column(name = "sonar_values")
    private String sonarValues;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_controllo", nullable = false)
    private TipoControllo tipoControllo;
    
}
