package com.soldati.models;

import java.math.BigDecimal;

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
@Table(name = "sonar_fact")
public class SonarFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_sonar_process", nullable = false)
    private SonarProcess sonarProcess;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_project", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_tipo_controllo", nullable = false)
    private TipoControllo tipoControllo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_severity")
    private SeveritySonar severity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_language")
    private LanguageSonar language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_type")
    private TypeSonar type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_iso_code")
    private IsoCode iso_code;

    @Column(name = "sonar_key")
    private String sonarKey;

    @Column(name = "sonar_values", columnDefinition = "TEXT")
    private String sonarValues;

    @Column(name = "fk_kpi_compliance")
    private String fkKpiCompliance;

    @Column(name = "fk_kpi_others")
    private String fkKpiOthers;

    @Column(name = "soglia_min", precision = 10, scale = 2)
    private BigDecimal sogliaMin;

    @Column(name = "soglia_max", precision = 10, scale = 2)
    private BigDecimal sogliaMax;

    @Column(name = "calc_compliance_c")
    private Integer calcCompliance;

    @Column(name = "fk_soglia")
    private Integer fkSoglia;

    @Column(name = "fk_tipo_inserimento", length = 2)
    private String fkTipoInserimento;

}
