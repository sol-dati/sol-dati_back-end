package com.soldati.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soldati.models.Project;
import com.soldati.models.Stato;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {

    List<Project> findByProjectNameOrderByTagDesc(String projectName);

    @Query("select p.tag from Project p where p.projectName = :projectName")
    Set<String> findTagSetByProjectName(@Param("projectName") String projectName);

    Optional<Project> findByProjectNameAndTag(String projectName, String tag);
    
 // tag con almeno un'analisi salvata e data dell'ultima, dalla più recente
    @Query("""
            select p, max(f.sonarProcess.processDate) from SonarFact f
            join f.project p
            group by p
            order by max(f.sonarProcess.processDate) desc
            """)
    List<Object[]> findAnalizzati();
    
    @Modifying(clearAutomatically = true)
    @Query("update Project p set p.stato = :a where p.stato = :da")
    int cambiaStato(@Param("da") Stato da, @Param("a") Stato a);
    
    //restituisce 0 se c'è già un'analisi in corso
    @Modifying(clearAutomatically = true)
    @Query("""
            update Project p set p.stato = :inCorso
            where p.projectName = :projectName and p.tag = :tag
              and (p.stato is null or p.stato <> :inCorso)
            """)
    int segnaInCorso(@Param("projectName") String projectName, @Param("tag") String tag, @Param("inCorso") Stato inCorso);
}
