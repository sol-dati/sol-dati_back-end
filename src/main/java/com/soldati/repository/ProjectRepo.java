package com.soldati.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soldati.models.Project;

@Repository
public interface ProjectRepo extends JpaRepository<Project, Long> {

    List<Project> findByProjectNameOrderByTagDesc(String projectName);

    @Query("select p.tag from Project p where p.projectName = :projectName")
    Set<String> findTagSetByProjectName(@Param("projectName") String projectName);

    Optional<Project> findByProjectNameAndTag(String projectName, String tag);
    
 // tag con almeno un'analisi salvata e data dell'ultima, dalla più recente (come l'elenco della dashboard Pentaho)
    @Query("""
            select p, max(f.sonarProcess.processDate) from SonarFact f
            join f.project p
            group by p
            order by max(f.sonarProcess.processDate) desc
            """)
    List<Object[]> findAnalizzati();
}
