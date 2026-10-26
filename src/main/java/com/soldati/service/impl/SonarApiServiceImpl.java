package com.soldati.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.soldati.exception.SoldatiException;
import com.soldati.repository.MetricaSqRepo;
import com.soldati.service.interfaces.SonarApiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//Interroga le API REST di SonarQube 
@Slf4j
@RequiredArgsConstructor
@Service
public class SonarApiServiceImpl implements SonarApiService {

    @Value("${sonar.host.url}")
    private String sonarHostUrl;

    @Value("${sonar.token}")
    private String sonarToken;

    private final RestTemplate restTemplate;
    private final MetricaSqRepo metricaSqRepo;

    // chiamata GET verso SonarQube (token come username, password vuota); il body può essere null
    private Map<String, Object> chiamaSonar(String url) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sonarToken, "");

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        return response.getBody();
    }

    // come sopra, ma se la risposta è vuota lancia l'errore con il messaggio indicato
    private Map<String, Object> chiamaSonar(String url, String messaggioRispostaVuota) {

        Map<String, Object> body = chiamaSonar(url);

        if (body == null) {
            throw new IllegalStateException(messaggioRispostaVuota);
        }

        return body;
    }

    // restituisce lo stato di elaborazione di un progetto
    
    @Override
    public boolean isRunning(String projectKey, Date startProcess) throws SoldatiException {

        String url = sonarHostUrl + "/api/ce/component?component=" + projectKey;

        log.debug("Controllo stato SonarQube API COMPONENT per projectKey={}", projectKey);

        Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per projectKey=" + projectKey);

        // current presente, un'elaborazione conclusa o in corso
        
        Map<String, Object> current = (Map<String, Object>) body.get("current");

        if (current != null) {
            return gestisciElaborazioneCorrente(current, projectKey, startProcess);
        }

        // Altrimenti verifica della coda di elaborazione

        return gestisciCoda(body, projectKey, startProcess);
    }

	private static boolean gestisciElaborazioneCorrente(Map<String, Object> current, String projectKey, Date startProcess) {

		String stato = (String) current.get("status");
        String dataInizioStr = (String) current.get("startedAt");

        Date dataInizio = Date.from(Instant.parse(dataInizioStr));

        if ("SUCCESS".equals(stato)) {

            // se troviamo un'elaborazione precedente al nostro avvio, l'analisi non è ancora arrivata

        	return !dataInizio.after(startProcess);
        }

        if ("RUNNING".equals(stato)) {
            return true;
        }

        if ("ERROR".equals(stato)) {
            throw new IllegalStateException("Analisi SonarQube in stato ERROR per projectKey=" + projectKey);
        }

        throw new IllegalStateException("Stato SonarQube non gestito: " + stato + " per projectKey=" + projectKey);
    }

    private static boolean gestisciCoda(Map<String, Object> body, String projectKey, Date startProcess) {

    	Object codaS = body.get("queue");

        if (!(codaS instanceof List<?> coda) || coda.isEmpty()) {

            // Nessuna elaborazione corrente / in coda
            return true;
        }

        Map<String, Object> elementoCoda = (Map<String, Object>) coda.get(0);
        String stato = (String) elementoCoda.get("status");

        if ("PENDING".equals(stato) || "IN_PROGRESS".equals(stato)) {

            String dataInvioStr = (String) elementoCoda.get("submittedAt");
            Date dataInvio = Date.from(Instant.parse(dataInvioStr));

            // precedente all'avvio quindi residuo di un'analisi precedente: errore
            if (startProcess.after(dataInvio)) {

                throw new IllegalStateException("Trovato processo PENDING/IN_PROGRESS precedente al nostro avvio per projectKey=" + projectKey);
            }
            return true;
        }
        return true;
    }


    
    // recupera le metriche principali 
   
    @Override
    public Map<String, Object> getResult(String projectKey) throws SoldatiException {

        List<String> metriche = metricaSqRepo.findAllMetriche();

        // Numero di metriche per ogni chiamata 
        final int dimensioneBlocco = 15;

        Map<String, Object> componentBase = null;
        List<Object> tutteLeMisure = new ArrayList<>();

        for (int i = 0; i < metriche.size(); i += dimensioneBlocco) {

            List<String> blocco = metriche.subList(i, Math.min(i + dimensioneBlocco, metriche.size()));

            Map<String, Object> body = chiamaMeasures(projectKey, blocco);

            Map<String, Object> component = (Map<String, Object>) body.get("component");

            if (component == null) {
                throw new IllegalStateException("Risposta SonarQube senza campo component, projectKey=" + projectKey);
            }

            // il primo component fa da base (key, name, qualifier...)
            if (componentBase == null) {
                componentBase = component;
            }

            // accumulo le misure di ogni blocco
            if (component.get("measures") instanceof List<?> misure) {
                tutteLeMisure.addAll(misure);
            }
        }

        if (componentBase == null) {
            throw new IllegalStateException("Nessuna metrica nel catalogo: sincronizzaMetriche() ha inserito qualcosa?");
        }

        // Rimetto tutte le misure insieme, con la stessa struttura di prima
        componentBase.put("measures", tutteLeMisure);

        Map<String, Object> risultato = new HashMap<>();
        risultato.put("component", componentBase);

        log.debug("Recuperate {} metriche per projectKey={}", tutteLeMisure.size(), projectKey);

        return risultato;
    }
    
    private Map<String, Object> chiamaMeasures(String projectKey, List<String> blocco) {

        String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
                .path("/api/measures/component")
                .queryParam("component", projectKey)
                .queryParam("metricKeys", String.join(",", blocco))
                .toUriString();

        return chiamaSonar(url, "Risposta vuota da SonarQube per metriche, projectKey=" + projectKey);
    }
    
    @Override
	public Map<String, Object> getRuleDetails(String ruleKey) throws SoldatiException {
    	
    	if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey non può essere vuota");
        }

        String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
                .path("/api/rules/show")
                .queryParam("key", ruleKey)
                .toUriString();

        log.debug("Chiamata API SonarQube RULES: ruleKey={}, url={}", ruleKey, url);

        Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per ruleKey=" + ruleKey);

        // ritorniamo l'oggetto "rule"
        Map<String, Object> rule = (Map<String, Object>) body.get("rule");

        if (rule == null) {
            throw new IllegalStateException("Regola non trovata su SonarQube: " + ruleKey);
        }

        return rule;
    }

	
	// ritorna le violazioni trovate da SonarQube per un progetto. 
	
	@Override
	public List<Map<String, Object>> getIssues(String projectKey) throws SoldatiException {

	    if (projectKey == null || projectKey.isBlank()) {
	        throw new IllegalArgumentException("projectKey non può essere vuota");
	    }

	    // Quante violazioni per pagina
	    final int pageSize = 500;

	    // numero massimo di pagine, per evitare loop
	    final int maxPagine = 50;

	    int page = 1;

	    // numero totale delle violazioni 
	    int total = 0;

	    // lista dove accumuliamo tutte le violazioni
	    List<Map<String, Object>> allIssues = new ArrayList<>();

	    
	    //la prima chiamata ci serve comunque per sapere il totale
	    
	    do {
	        String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
	                .path("/api/issues/search")
	                .queryParam("componentKeys", projectKey)
	                .queryParam("p", page)
	                .queryParam("ps", pageSize)
	                .toUriString();

	        log.debug("Chiamata API SonarQube ISSUE: projectKey={}, pagina={}", projectKey, page);

	        Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per issue, projectKey=" + projectKey);

	        // il numero totale di violazioni 
	        Object totalObject = body.get("total");

	        if (!(totalObject instanceof Number)) {
	            throw new IllegalStateException("Campo 'total' assente o non numerico nella risposta issue, projectKey=" + projectKey);
	        }

	        total = ((Number) totalObject).intValue();

	        // la lista di violazioni della pagina
	        Object issuesObject = body.get("issues");

	        if (!(issuesObject instanceof List<?> issuesPage)) {
	            throw new IllegalStateException("Campo 'issues' assente o non valido nella risposta SonarQube, projectKey=" + projectKey);
	        }

	        if (issuesPage.isEmpty() && allIssues.size() < total) {
	            throw new IllegalStateException("SonarQube ha restituito una pagina vuota prima di raggiungere il totale atteso, projectKey=" + projectKey);
	        }

	        // aggiungiamo le violazioni alla lista allIssues
	        for (Object issue : issuesPage) {
	            if (issue instanceof Map<?, ?> issueMap) {
	                allIssues.add((Map<String, Object>) issueMap);
	            }
	        }

	        // pagina successiva 
	        page++;

	        if (page > maxPagine) {throw new IllegalStateException("Superato il numero massimo di pagine (" + maxPagine + ") durante il recupero issue, projectKey=" + projectKey);
	        }

	    // si ripete finché non abbiamo tante violazioni quante il totale 
	    } while (allIssues.size() < total);

	    log.debug("Recuperate {} issue SonarQube per projectKey={}", allIssues.size(), projectKey);

	    return allIssues;
	}
	
	// Servono per salvare a 0 le regole controllate da Sonar ma senza violazioni.
	@Override
	public Set<String> getRegoleAttive(String projectKey) throws SoldatiException {

	    if (projectKey == null || projectKey.isBlank()) {
	        throw new IllegalArgumentException("projectKey non può essere vuota");
	    }

	    // 1. quality profile usati dal progetto
	    String urlProfili = UriComponentsBuilder.fromUriString(sonarHostUrl)
	            .path("/api/qualityprofiles/search")
	            .queryParam("project", projectKey)
	            .toUriString();

	    Map<String, Object> bodyProfili = chiamaSonar(urlProfili);

	    Set<String> regole = new LinkedHashSet<>();

	    if (bodyProfili == null || !(bodyProfili.get("profiles") instanceof List<?> profili)) {
	        log.warn("Nessun quality profile trovato per projectKey={}", projectKey);
	        return regole;
	    }

	    // 2. regole attive di ogni profilo (paginate, al massimo 500 per pagina)
	    final int pageSize = 500;
	    final int maxPagine = 20;

	    for (Object p : profili) {
	        if (!(p instanceof Map<?, ?> profilo) || profilo.get("key") == null) {
	            continue;
	        }
	        String chiaveProfilo = profilo.get("key").toString();
	        int page = 1;
	        int total;

	        do {
	            String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
	                    .path("/api/rules/search")
	                    .queryParam("qprofile", chiaveProfilo)
	                    .queryParam("activation", "true")
	                    .queryParam("f", "lang")
	                    .queryParam("ps", pageSize)
	                    .queryParam("p", page)
	                    .toUriString();

	            Map<String, Object> body = chiamaSonar(url);

	            if (body == null || !(body.get("rules") instanceof List<?> pagina)) {
	                break;
	            }
	            total = body.get("total") instanceof Number n ? n.intValue() : 0;

	            for (Object r : pagina) {
	                if (r instanceof Map<?, ?> regola && regola.get("key") != null) {
	                    regole.add(regola.get("key").toString());
	                }
	            }
	            page++;

	        } while ((page - 1) * pageSize < total && page <= maxPagine);
	    }

	    log.debug("Regole attive per projectKey={}: {}", projectKey, regole.size());

	    return regole;
	}

	//elenco delle metriche disponibili

	@Override
	public List<Map<String, Object>> getAvailableMetrics() throws SoldatiException {

	    String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
	            .path("/api/metrics/search")
	            .queryParam("ps", 500)
	            .toUriString();

	    log.debug("Chiamata API SonarQube METRICS, url={}", url);

	    Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per /api/metrics/search");

	    Object metricsObject = body.get("metrics");

	    if (!(metricsObject instanceof List<?>)) {
	        throw new IllegalStateException( "Campo 'metrics' assente o non valido nella risposta SonarQube");
	    }
	    return (List<Map<String, Object>>) metricsObject;
	}



	@Override
	public List<Map<String, Object>> getAvailableLanguages() throws SoldatiException {
		
		String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
	            .path("/api/languages/list")
	            .toUriString();

	    log.debug("Chiamata API SonarQube LANGUAGES, url={}", url);

	    Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per /api/languages/list");

	    Object languagesObject = body.get("languages");

	    if (!(languagesObject instanceof List<?>)) {
	        throw new IllegalStateException("Campo 'languages' assente o non valido nella risposta SonarQube");
	    }

	    return (List<Map<String, Object>>) languagesObject;
	}



	@Override
	public List<String> getAvailableTypes() throws SoldatiException {
		
		Set<String> tipi = new LinkedHashSet<>(getFacetValues("types"));

	    for (Map<String, Object> metrica : getAvailableMetrics()) {
	        Object tipo = metrica.get("type");
	        if (tipo != null && !tipo.toString().isBlank()) {
	            tipi.add(tipo.toString());
	        }
	    }

	    return new ArrayList<>(tipi);
	}

	private List<String> getFacetValues(String facet) {

	    String url = UriComponentsBuilder.fromUriString(sonarHostUrl)
	            .path("/api/rules/search")
	            .queryParam("facets", facet)
	            .queryParam("ps", 1)
	            .toUriString();

	    log.debug("Chiamata API SonarQube RULES facet={}, url={}", facet, url);

	    Map<String, Object> body = chiamaSonar(url, "Risposta vuota da SonarQube per /api/rules/search, facet=" + facet);

	    if (!(body.get("facets") instanceof List<?> facets)) {
	        throw new IllegalStateException("Campo 'facets' assente nella risposta SonarQube, facet=" + facet);
	    }

	    List<String> valori = new ArrayList<>();

	    for (Object facetObject : facets) {
	        if (facetObject instanceof Map<?, ?> facetMap && facet.equals(facetMap.get("property")) 
	        		&& facetMap.get("values") instanceof List<?> values) {

	            for (Object valueObject : values) {
	                if (valueObject instanceof Map<?, ?> valueMap && valueMap.get("val") != null) {
	                    valori.add(valueMap.get("val").toString());
	                }
	            }
	        }
	    }
	    return valori;
	}



	@Override
	public List<String> getAvailableSeverities() throws SoldatiException {
		
		Set<String> severities = new LinkedHashSet<>(getFacetValues("severities"));

	    try {
	        severities.addAll(getFacetValues("impactSeverities"));
	    } catch (Exception e) {
	        log.warn("Facet impactSeverities non disponibile su questa istanza SonarQube: {}", e.getMessage());
	    }

	    return new ArrayList<>(severities);
	}
}