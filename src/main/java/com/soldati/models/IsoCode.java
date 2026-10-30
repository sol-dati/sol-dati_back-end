package com.soldati.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "iso25010code")
public class IsoCode {

    @Id
    @Column(nullable = false, unique = true)
    private String iso_code;

    @Column(nullable = false)
    private String description;
}
