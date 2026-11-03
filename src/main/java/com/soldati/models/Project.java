package com.soldati.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.DynamicInsert;

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
@DynamicInsert
@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false, length = 200)
    private String projectName;

    @Column(name = "release_number")
    private Long releaseNumber;

    @Column(name = "sprint_number")
    private Long sprintNumber;

    @Column(name = "tag", nullable = false, length = 500)
    private String tag;

    @Column(name = "data_inserimento", nullable = false)
    private LocalDate dataInserimento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_stato_sq")
    private Stato stato;

    // data dell'ultimo invio dei KPI manuali del tag
    @Column(name = "data_invio_manuali")
    private LocalDateTime dataInvioManuali;
}
