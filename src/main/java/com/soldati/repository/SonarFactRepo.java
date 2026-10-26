package com.soldati.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.soldati.models.SonarFact;

@Repository
public interface SonarFactRepo extends JpaRepository<SonarFact, Long>{
	
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
}
