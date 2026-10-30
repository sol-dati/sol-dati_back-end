package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.Stato;

@Repository
public interface StatoRepo extends JpaRepository<Stato, Long> {
}
