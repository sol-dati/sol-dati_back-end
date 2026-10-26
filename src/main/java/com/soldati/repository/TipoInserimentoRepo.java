package com.soldati.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.TipoInserimento;

@Repository
public interface TipoInserimentoRepo extends JpaRepository<TipoInserimento, String>{
	
	Optional<TipoInserimento> findByDescrizione(String descrizione);

}
