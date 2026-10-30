package com.soldati.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.soldati.models.MetricaSq;

@Repository
public interface MetricaSqRepo extends JpaRepository<MetricaSq, Long> {

    boolean existsByMetrica(String metrica);

    @Query("SELECT m.metrica FROM MetricaSq m ORDER BY m.id")
    List<String> findAllMetriche();
}
