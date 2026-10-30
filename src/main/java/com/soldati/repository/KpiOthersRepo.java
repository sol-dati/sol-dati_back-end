package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.KpiOthers;

@Repository
public interface KpiOthersRepo extends JpaRepository<KpiOthers, Long> {
}
