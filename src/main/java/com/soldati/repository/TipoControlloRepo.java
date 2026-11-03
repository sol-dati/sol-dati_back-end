package com.soldati.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.TipoControllo;

@Repository
public interface TipoControlloRepo extends JpaRepository<TipoControllo, Long> {

    Optional<TipoControllo> findBySonarControlType(String sonarControlType);
}
