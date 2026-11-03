package com.soldati.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.SeveritySonar;

@Repository
public interface SeveritySonarRepo extends JpaRepository<SeveritySonar, Long> {
    boolean existsBySeverity(String severity);

    Optional<SeveritySonar> findBySeverity(String severity);
}
