# Progetto Soldati – refactoring Pentaho → Java

## Contesto
- Migrazione di un vecchio ETL Pentaho (Kettle: file .kjb/.ktr) verso un backend Spring Boot (`com.soldati`).
- Il frontend è ancora quello di Pentaho e va collegato alle API REST del backend Java.
- Prima c'erano due DB (MySQL `kpi`/qmap + Postgres `pentaho` con schemi `ods` ed `edw`).
  Ora c'è un **unico DB MySQL `kpilocal`** con le tabelle:
  `deco_stato, iso25010code, language, metriche_sq, project, severity, sonar_fact,
  sonar_kpi, sonar_process, sqm_gestione_kpi, tipo_controllo, type`.

## Flusso del backend
1. `ProjectController /rest/project/sync`: legge progetti e tag da GitLab e popola `project`.
2. `SonarScanController /rest/sonar/avvia`: scarica i sorgenti del tag da GitLab, lancia Maven + Sonar,
   attende SonarQube, sincronizza i cataloghi (metriche, language, type, severity),
   salva `sonar_process`, `sonar_kpi` e `sonar_fact`.
3. `/rest/sonar/fact/{projectId}`: restituisce i risultati raggruppati per codice ISO 25010.

## Corrispondenza vecchio ETL → nuove tabelle
| Pentaho (edw) | kpilocal |
|---|---|
| dim_severity | severity |
| dim_type | type |
| dim_tipo_controllo (M=Metriche, V=Valore Manuale, R=Regole) | tipo_controllo |
| dim_cat_lang (categorie: Altro / Linguaggio / Quality Assurance) | language |
| dim_projects (label "Release X"/"Sprint Y", sprint_order_key con sprint 0 → 'Z', filtro id_stato_sq = 2) | project |
| fact_kpi_25010 | sonar_fact |
| dim_kpi_compliance / dim_kpi_others | metriche_sq (+ iso) |

## Codici ISO 25010 (tabella iso25010code)
A Affidabilità · C Compatibilità · E Efficienza · EX Extra ISO · IF Idoneità funzionale ·
M Manutenibilità · P Portabilità · S Sicurezza · U Usabilità · X Non definito

Assegnazione desiderata (cascata), oggi in `SonarSaveMapper.risolviCodiceIso`:
1. Override da DB per chiave (colonna `iso_code` su `metriche_sq` o tabella dedicata).
2. Regole: tag (`performance`→E, `accessibility`→U, `cross-browser`→C), poi `impacts[].softwareQuality`
   (SECURITY→S, RELIABILITY→A, MAINTAINABILITY→M), infine il `type` legacy.
   `SonarScanMapper.buildRule` deve leggere anche `tags` e `impacts` da `/api/rules/show`.
3. Metriche: mappa per chiave (bugs→A, vulnerabilities/security_*→S, code_smells/sqale_*→M,
   alert_status/ncloc/files→EX), poi per `domain`.
4. Default X.

## Problemi noti da sistemare
- `/rest/sonar/avvia` è sincrono (fino a più di 5 minuti): renderlo asincrono e far interrogare lo stato al frontend.
- `getUltimoRisultato` restituisce vuoto se l'analisi non è di oggi (`eDiOggi`).
- Manca la configurazione CORS per il frontend Pentaho.
- Manca un `@RestControllerAdvice`: oggi gli errori sono stringhe con codice 400.
- GitLab: l'header `TOKEN` va sostituito con `PRIVATE-TOKEN`.
- `getIssues`: aggiungere `resolved=false`; meglio ancora usare `facets=rules`, perché c'è il limite di 10.000 issue.
- `salvaRisultati` e `salvaFact` girano in transazioni separate (`salvaFact` non è @Transactional) e ricalcolano due volte i dati.
- `salvaFact` inserisce il domain delle metriche nella tabella `language` (il commento dice il contrario).
- I campi release, sprint e stato di `Project` non vengono mai valorizzati.
- I campi `iso_code` con underscore rompono le query derivate di Spring Data: usare `isoCode` + @Column.

## Convenzioni
- Codice e commenti in italiano.
- Credenziali solo in application.properties o variabili d'ambiente, mai nel codice.
