package com.soldati.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.soldati.models.LanguageSonar;

public interface LanguageSonarRepo extends JpaRepository<LanguageSonar, Long>{
	
    Optional<LanguageSonar> findByLanguage(String language);

}
