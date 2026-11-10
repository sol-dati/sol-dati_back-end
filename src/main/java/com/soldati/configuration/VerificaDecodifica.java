package com.soldati.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.soldati.enums.EsitoSoglia;
import com.soldati.enums.Inserimento;
import com.soldati.enums.StatoAnalisi;
import com.soldati.enums.Controllo;
import com.soldati.repository.SogliaRepo;
import com.soldati.repository.StatoRepo;
import com.soldati.repository.TipoControlloRepo;
import com.soldati.repository.TipoInserimentoRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// all'avvio controlla che ogni valore degli enum abbia la sua riga nelle tabelle del db
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class VerificaDecodifica implements ApplicationRunner {

    private final StatoRepo statoRepo;
    private final SogliaRepo sogliaRepo;
    private final TipoInserimentoRepo tipoInserimentoRepo;
    private final TipoControlloRepo tipoControlloRepo;

    @Override
    public void run(ApplicationArguments args) {

        List<String> mancanti = new ArrayList<>();

        for (StatoAnalisi s : StatoAnalisi.values()) {
            if (!statoRepo.existsById(s.getIdStato())) {
                mancanti.add("deco_stato." + s + " (id " + s.getIdStato() + ")");
            }
        }
        for (EsitoSoglia e : EsitoSoglia.values()) {
            if (!sogliaRepo.existsById(e.getIdSoglia())) {
                mancanti.add("soglie." + e + " (id " + e.getIdSoglia() + ")");
            }
        }
        for (Inserimento i : Inserimento.values()) {
            if (!tipoInserimentoRepo.existsById(i.getIdTipo())) {
                mancanti.add("tipo_inserimento." + i + " (id " + i.getIdTipo() + ")");
            }
        }
        for (Controllo c : Controllo.values()) {
            if (tipoControlloRepo.findBySonarControlType(c.getCodice()).isEmpty()) {
                mancanti.add("tipo_controllo." + c + " (tipo " + c.getCodice() + ")");
            }
        }

        if (!mancanti.isEmpty()) {
            throw new IllegalStateException("Righe mancanti nelle tabelle di decodifica: " + mancanti);
        }
        log.info("Tabelle di decodifica verificate");
    }
}