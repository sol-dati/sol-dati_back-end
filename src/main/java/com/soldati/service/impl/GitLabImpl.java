package com.soldati.service.impl;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.soldati.dto.output.GitLabProjectDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.GitLabMapper;
import com.soldati.util.FileUtils;
import com.soldati.service.interfaces.GitLabService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class GitLabImpl implements GitLabService{

    @Value("${gitlab.url}")
    private String gitlabUrl;

    @Value("${gitlab.token}")
    private String gitlabToken;

    private final RestTemplate restTemplate;

    private HttpEntity<Void> autenticazione() {

        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", gitlabToken);

        return new HttpEntity<>(headers);
    }

    // chiamata GET verso GitLab che restituisce una lista di oggetti JSON; il body può essere null
    private List<Map<String, Object>> chiamaGitLab(String url) {

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        autenticazione(),
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

        return response.getBody();
    }

    //Chiamata usata per popolare la select nel frontend.
    
    @Override
    public List<String> getProjectNames() throws SoldatiException {

        final int perPage = 100;
        
        final int maxPagine = 20;

        int page = 1;

        // lista finale con solo i nomi
        List<String> projectNames = new ArrayList<>();

        while (page <= maxPagine) {

            // membership=true: solo i progetti di cui l'utente del token è membro
            String url = gitlabUrl + "/api/v4/projects?membership=true&simple=true&per_page=" + perPage + "&page=" + page;

            log.debug("Recupero progetti da GitLab, pagina={}", page);

            List<Map<String, Object>> progettiPagina = chiamaGitLab(url);

            // pagina vuota
            if (progettiPagina == null || progettiPagina.isEmpty()) {
                break;
            }

            for (Map<String, Object> project : progettiPagina) {
                Object name = project.get("name");

                if (name != null) {
                    // i nomi in minuscolo
                    projectNames.add(name.toString().toLowerCase(Locale.ROOT));
                }
            }

            // meno elementi del massimo: era l'ultima pagina
            if (progettiPagina.size() < perPage) {
                break;
            }

            page++;
        }

        log.debug("Recuperati {} progetti da GitLab", projectNames.size());

        return projectNames;
    }
    
    // Recupera i tag di un progetto GitLab.

    @Override
    public List<String> getTags(String projectName) throws SoldatiException {

        // id del progetto, che serve per chiamare le API dei tag
        GitLabProjectDTO project = findProjectByName(projectName);

        final int perPage = 100;

        // Rete di sicurezza contro cicli infiniti (max 20 pagine = 2000 tag)
        final int maxPagine = 20;

        int page = 1;

        // Lista finale con i soli nomi dei tag
        List<String> tagNames = new ArrayList<>();

        while (page <= maxPagine) {

            String url = gitlabUrl + "/api/v4/projects/" + project.getId() + "/repository/tags?per_page=" + perPage + "&page=" + page;

            log.debug("Recupero tag GitLab: projectId={}, pagina={}", project.getId(), page);

            List<Map<String, Object>> tagsPagina = chiamaGitLab(url);

            // pagina vuota
            if (tagsPagina == null || tagsPagina.isEmpty()) {
                break;
            }

            // di ogni tag ci interessa solo il nome
            for (Map<String, Object> tag : tagsPagina) {
                Object name = tag.get("name");

                if (name != null) {
                    tagNames.add(name.toString());
                }
            }

            // Se la pagina ha meno elementi del massimo, era l'ultima
            if (tagsPagina.size() < perPage) {
                break;
            }

            page++;
        }

        log.debug("Recuperati {} tag per il progetto {}", tagNames.size(), projectName);

        return tagNames;
    }

    
    //Cerca un progetto dal nome
     
    @Override
    public GitLabProjectDTO findProjectByName(String projectName) throws SoldatiException {

        if (projectName == null || projectName.isBlank()) {
            throw new IllegalArgumentException("projectName non può essere vuoto");
        }

        // GitLab cerca i progetti che contengono questa stringa nel nome
        String url = gitlabUrl + "/api/v4/projects?membership=true&simple=true&search=" + projectName;

        log.debug("Ricerca progetto GitLab per nome: {}", projectName);

        List<Map<String, Object>> progetti = chiamaGitLab(url);

        if (progetti == null || progetti.isEmpty()) {
            throw new IllegalStateException("Nessun progetto GitLab trovato per nome: " + projectName);
        }

        // risultati trovati da GitLab, cerchiamo quello che corrisponde al nome cercato
        for (Map<String, Object> project : progetti) {
            Object name = project.get("name");

            if (name != null && name.toString().equalsIgnoreCase(projectName)) {

                GitLabProjectDTO gitLab = GitLabMapper.buildGitLabProjectDTO(project);

                log.info("Progetto GitLab trovato: id={}, path={}", gitLab.getId(), gitLab.getPathWithNamespace());

                return gitLab;
            }
        }

        //  GitLab ha restituito risultati "simili" ma nessuno corrisponde esattamente al nome cercato
        throw new IllegalStateException("Nessuna corrispondenza esatta trovata per nome: " + projectName);
    }

    //Scarica il codice sorgente di un tag specifico da GitLab,

    @Override
    public File fetchSourceForTag(Long projectId, String tag) throws Exception {

        if (projectId == null) {
            throw new IllegalArgumentException("projectId non può essere null");
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("tag non può essere vuoto");
        }

        // Il nome del tag codificato per l'URL
        
        String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8);

        // ritorna file zip con i sorgenti del tag scelto
        
        String url = gitlabUrl + "/api/v4/projects/" + projectId
                + "/repository/archive.zip?sha=" + encodedTag;

        log.debug("Download archive GitLab: projectId={}, tag={}", projectId, tag);

        // file zip scaricato in byte grezzi

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                autenticazione(),
                byte[].class
        );

        if (response.getBody() == null || response.getBody().length == 0) {
            throw new IllegalStateException("Archive vuoto o non trovato per projectId=" + projectId + ", tag=" + tag);
        }

        // Creiamo una cartella temporanea
        
        Path cartellaTemporanea = Files.createTempDirectory(FileUtils.PREFISSO_CARTELLA_TEMPORANEA);

        // Salviamo lo zip dentro la cartella
        
        Path fileZip = cartellaTemporanea.resolve("source.zip");
        Files.write(fileZip, response.getBody());

        // Cartella dove verrà estratto lo zip
        
        Path cartellaEstratta = cartellaTemporanea.resolve("extracted");
        Files.createDirectories(cartellaEstratta);

        FileUtils.estraiZip(fileZip, cartellaEstratta);

        // Lo zip contiene una sotto cartella con il progetto, qui troviamo la cartella per passarla a Maven
        File cartellaProgetto = FileUtils.trovaCartellaProgetto(cartellaEstratta);

        log.debug("Sorgenti estratti in: {}", cartellaProgetto.getAbsolutePath());

        return cartellaProgetto;
    }
}
