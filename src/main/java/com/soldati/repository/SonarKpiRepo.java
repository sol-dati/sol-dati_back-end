package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.SonarKpi;

@Repository
public interface SonarKpiRepo extends JpaRepository<SonarKpi, Long> {
}
