package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.SonarProcess;

@Repository
public interface SonarProcessRepo extends JpaRepository<SonarProcess, Long> {
}
