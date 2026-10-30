package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.KpiCompliance;

@Repository
public interface KpiComplianceRepo extends JpaRepository<KpiCompliance, Long> {
}
