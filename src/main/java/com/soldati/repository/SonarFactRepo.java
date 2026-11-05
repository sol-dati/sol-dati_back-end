package com.soldati.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soldati.models.SonarFact;

@Repository
public interface SonarFactRepo extends JpaRepository<SonarFact, Long> {

    @Query("""
            select f from SonarFact f
            join fetch f.tipoControllo
            left join fetch f.severity
            left join fetch f.language
            left join fetch f.type
            where f.project.id = :projectId
              and f.sonarProcess.id = (select max(f2.sonarProcess.id) from SonarFact f2
                                       where f2.project.id = :projectId)
            """)
        List<SonarFact> findUltimaAnalisi(@Param("projectId") Long projectId);

    // righe collegate al catalogo kpi_compliance dell'ultima analisi di ogni tag (calcolo della compliance)
    @Query("""
            select f from SonarFact f
            where f.fkKpiCompliance is not null
              and f.sonarProcess.id = (select max(f2.sonarProcess.id) from SonarFact f2
                                       where f2.project.id = f.project.id)
            """)
    List<SonarFact> findUltimeAnalisiCompliance();

    // id dell'ultima analisi che ha righe per il tag
    @Query("select max(f.sonarProcess.id) from SonarFact f where f.project.id = :projectId")
    Optional<Long> findUltimaAnalisiId(@Param("projectId") Long projectId);

    // righe di un'analisi con un certo tipo di controllo
    @Query("select f from SonarFact f where f.sonarProcess.id = :processoId and f.tipoControllo.sonarControlType = :tipo")
    List<SonarFact> findByProcessoAndTipo(@Param("processoId") Long processoId, @Param("tipo") String tipo);

    // ultima analisi del tag che ha righe di quel tipo di controllo
    @Query("""
            select max(f.sonarProcess.id) from SonarFact f
            where f.project.id = :projectId
              and f.tipoControllo.sonarControlType = :tipo
              and f.sonarProcess.id < :processoId
            """)
    Optional<Long> findAnalisiPrecedenteConTipo(@Param("projectId") Long projectId, @Param("tipo") String tipo,
            @Param("processoId") Long processoId);
}
