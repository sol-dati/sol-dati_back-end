package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.TypeSonar;

@Repository
public interface TypeSonarRepo extends JpaRepository<TypeSonar, Long> {

    boolean existsByType(String type);
}
