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
}
