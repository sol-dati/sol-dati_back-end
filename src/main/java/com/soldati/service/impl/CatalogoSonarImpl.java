package com.soldati.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.soldati.dto.input.SonarKpiReq;
import com.soldati.exception.SoldatiException;
import com.soldati.models.LanguageSonar;
import com.soldati.models.MetricaSq;
import com.soldati.models.SeveritySonar;
import com.soldati.models.TipoControllo;
import com.soldati.models.TypeSonar;
import com.soldati.repository.LanguageSonarRepo;
import com.soldati.repository.MetricaSqRepo;
import com.soldati.repository.SeveritySonarRepo;
import com.soldati.repository.TypeSonarRepo;
import com.soldati.service.interfaces.CatalogoSonarService;
import com.soldati.service.interfaces.SonarApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// legge i cataloghi da SonarQube (tramite SonarApiService) e li salva sul DB
@Slf4j
@RequiredArgsConstructor
@Service
public class CatalogoSonarImpl implements CatalogoSonarService {

    private final SonarApiService sonarApiService;
    private final MetricaSqRepo metricaSqRepo;
    private final LanguageSonarRepo languageRepo;
    private final TypeSonarRepo typeRepo;
    private final SeveritySonarRepo severityRepo;

    @Override
    public int sincronizzaMetriche() throws SoldatiException {

        List<Map<String, Object>> metricheSonar = sonarApiService.getAvailableMetrics();

        int inserite = 0;

        for (Map<String, Object> metricaSonar : metricheSonar) {

            Object keyObject = metricaSonar.get("key");

            if (keyObject == null) {
                continue;
            }

            String key = keyObject.toString();

            if (metricaSqRepo.existsByMetrica(key)) {
                continue;
            }

            MetricaSq metrica = new MetricaSq();
            metrica.setMetrica(key);

            metricaSqRepo.save(metrica);

            inserite++;
        }

        log.info("Sincronizzazione metriche SonarQube completata. Nuove metriche: {}", inserite);

        return inserite;
    }

    @Override
    public int sincronizzaLanguage() throws SoldatiException {

        List<Map<String, Object>> languagesSonar = sonarApiService.getAvailableLanguages();

        int inseriti = 0;

        for (Map<String, Object> languageSonar : languagesSonar) {

            Object keyObject = languageSonar.get("key");

            if (keyObject == null || keyObject.toString().isBlank()) {
                continue;
            }

            String key = keyObject.toString();
            String name = languageSonar.get("name") != null ? languageSonar.get("name").toString() : null;

            Optional<LanguageSonar> esistente = languageRepo.findByLanguage(key);

            if (esistente.isPresent()) {
                LanguageSonar language = esistente.get();
                if (name != null && !name.equals(language.getName())) {
                    language.setName(name);
                    languageRepo.save(language);
                }
                continue;
            }

            LanguageSonar language = new LanguageSonar();
            language.setLanguage(key);
            language.setName(name);
            languageRepo.save(language);

            inseriti++;
        }

        log.info("Sincronizzazione language SonarQube completata. Nuovi linguaggi: {}", inseriti);

        return inseriti;
    }

    @Override
    public int sincronizzaType() throws SoldatiException {
        List<String> typesSonar = sonarApiService.getAvailableTypes();

        int inseriti = 0;

        for (String typeSonar : typesSonar) {

            if (typeSonar == null || typeSonar.isBlank()) {
                continue;
            }

            if (typeRepo.existsByType(typeSonar)) {
                continue;
            }

            TypeSonar type = new TypeSonar();
            type.setType(typeSonar);
            typeRepo.save(type);

            inseriti++;
        }

        log.info("Sincronizzazione type SonarQube completata. Nuovi type: {}", inseriti);

        return inseriti;
    }

    @Override
    public int sincronizzaSeverity() throws SoldatiException {
        List<String> severitiesSonar = sonarApiService.getAvailableSeverities();

        int inserite = 0;

        for (String severitySonar : severitiesSonar) {

            if (severitySonar == null || severitySonar.isBlank()) {
                continue;
            }

            if (severityRepo.existsBySeverity(severitySonar)) {
                continue;
            }

            SeveritySonar severity = new SeveritySonar();
            severity.setSeverity(severitySonar);
            severityRepo.save(severity);

            inserite++;
        }

        log.info("Sincronizzazione severity SonarQube completata. Nuove severity: {}", inserite);

        return inserite;
    }

    @Override
    public Map<String, LanguageSonar> leggiLanguage(List<SonarKpiReq> righe) {

        Map<String, LanguageSonar> languages = languageRepo.findAll().stream()
                .collect(Collectors.toMap(LanguageSonar::getLanguage, Function.identity()));

        for (SonarKpiReq r : righe) {
            if (TipoControllo.TIPO_METRICA.equals(r.getSonarControlType())
                    && r.getLanguage() != null
                    && !languages.containsKey(r.getLanguage())) {

                LanguageSonar nuovo = new LanguageSonar();
                nuovo.setLanguage(r.getLanguage());
                languages.put(nuovo.getLanguage(), languageRepo.save(nuovo));
            }
        }

        return languages;
    }
}
