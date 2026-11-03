package com.soldati.models;

import java.math.BigDecimal;

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
@Table(name = "kpi_compliance")
public class KpiCompliance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_kpi_cod", length = 2)
    private String typeKpiCod;

    @Column(name = "key_kpi_c", length = 200, unique = true)
    private String keyKpi;

    @Column(name = "kpi_name_c", length = 200, nullable = false)
    private String kpiName;

    @Column(name = "iso_c", nullable = false)
    private String isoCode;

    @Column(name = "target_min_c", precision = 10, scale = 2)
    private BigDecimal targetMin;

    @Column(name = "target_max_c", precision = 10, scale = 2)
    private BigDecimal targetMax;

    @Column(name = "calc_compliance_c")
    private Boolean calcCompliance;

    @Column(name = "severity_c", length = 20)
    private String severity;

    @Column(name = "description_c", columnDefinition = "TEXT")
    private String description;
}
