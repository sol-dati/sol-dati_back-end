package com.soldati.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.Soglia;

@Repository
public interface SogliaRepo extends JpaRepository<Soglia, Integer> {

    Optional<Soglia> findByDescrizione(String descrizione);
}
