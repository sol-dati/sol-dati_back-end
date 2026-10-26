package com.soldati.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.soldati.models.IsoCode;

@Repository
public interface IsoCodeRepo extends JpaRepository<IsoCode, String>{

}
