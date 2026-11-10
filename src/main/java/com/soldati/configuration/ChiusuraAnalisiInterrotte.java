package com.soldati.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.soldati.enums.StatoAnalisi;
import com.soldati.repository.StatoRepo;
import com.soldati.service.interfaces.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// all'avvio ogni stato deve avere la sua riga in deco_stato e nessuna analisi può essere in corso e passa a errore
@Slf4j
@Order(2)
@Component
@RequiredArgsConstructor
public class ChiusuraAnalisiInterrotte implements ApplicationRunner {

    private final StatoRepo statoRepo;
    private final ProjectService projectService;

    @Override
    public void run(ApplicationArguments args) {

        List<StatoAnalisi> mancanti = Arrays.stream(StatoAnalisi.values())
                .filter(s -> !statoRepo.existsById(s.getIdStato()))
                .toList();
        if (!mancanti.isEmpty()) {
            throw new IllegalStateException("Righe mancanti in deco_stato per: " + mancanti);
        }

        int tag = projectService.chiudiAnalisiInterrotte();
        if (tag > 0) {
            log.warn("{} tag rimasti 'In corso' da un'esecuzione interrotta: impostati a 'Errore'", tag);
        }
    }
}