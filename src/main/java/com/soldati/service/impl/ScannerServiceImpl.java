package com.soldati.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.soldati.util.FileUtils;
import com.soldati.service.interfaces.ScannerService;

import lombok.extern.slf4j.Slf4j;

// Analisi Sonar con lo scanner adatto al progetto:
// - pom.xml nella cartella principale -> SonarScanner for Maven (compila e usa classpath e classi)
// - nessun pom.xml principale         -> SonarScanner CLI (analizza la cartella così com'è)
@Slf4j
@Service
public class ScannerServiceImpl implements ScannerService {

	// dove è installato maven sul server
    @Value("${maven.home}")
    private String mavenHome;

    //comandi maven da eseguire
    @Value("${maven.command.parameter}")
    private String mavenCommandParameter;

    // parametri per analizzare tutta la cartella (sorgenti ed esclusioni):
    // usati per i progetti Maven senza src/main/java (es. Python) e per tutti quelli analizzati con la CLI
    @Value("${maven.command.parameter.non.java}")
    private String mavenCommandParameterNonJava;

    // settings.xml di Maven con il repository aziendale (facoltativo: vuoto = settings di default)
    @Value("${maven.settings.file:}")
    private String mavenSettingsFile;

    // dove è installato SonarScanner CLI sul server (la cartella che contiene bin/)
    @Value("${sonar.scanner.home}")
    private String sonarScannerHome;

    // parametri aggiuntivi per SonarScanner CLI, separati da virgola
    @Value("${sonar.scanner.parameter:}")
    private String sonarScannerParameter;

    // server e token sonar
    @Value("${sonar.host.url}")
    private String sonarHostUrl;

    @Value("${sonar.token}")
    private String sonarToken;

    @Override
    public int runSonarAnalisi(File sourceDir, String projectKey) throws Exception {

    	if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("sourceDir non valida: " + sourceDir);
        }
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey non può essere vuota");
        }

        int exitCode = FileUtils.haPomPrincipale(sourceDir.toPath())
                ? analisiMaven(sourceDir, projectKey)
                : analisiScannerCli(sourceDir, projectKey);

        if (exitCode != 0) {
            throw new IllegalStateException("Analisi Sonar terminata con exit code: " + exitCode + " per projectKey=" + projectKey);
        }
        log.debug("Analisi Sonar completata con successo per projectKey={}", projectKey);

        return exitCode;
    }

    // progetto Maven
    private int analisiMaven(File sourceDir, String projectKey) throws Exception {

        log.debug("Avvio analisi Maven/Sonar per sourceDir={}, projectKey={}", sourceDir, projectKey);

        // i pom con sonar.password/sonar.login fanno fallire l'analisi: li tolgo dalla copia scaricata
        FileUtils.rimuoviCredenzialiSonar(sourceDir.toPath());

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(sourceDir);   // cartella dei sorgenti scaricati
        request.setBatchMode(true);            // esecuzione senza interazione

        if (!mavenSettingsFile.isBlank()) {
            request.setUserSettingsFile(new File(mavenSettingsFile));
            log.debug("Uso il settings Maven {}", mavenSettingsFile);
        }

        // sostituiamo "?" con la projectKey e lo aggiungiamo a maven
        for (String comando : mavenCommandParameter.split(",")) {
            request.addArg(comando.replace("?", "-Dsonar.projectKey=" + projectKey));
        }

        if (!FileUtils.haSorgentiJava(sourceDir.toPath())) {
            log.info("Nessuna cartella src/main/java per projectKey={}: analizzo tutta la cartella del progetto", projectKey);
            for (String comando : mavenCommandParameterNonJava.split("\\|")) {
                request.addArg(comando);
            }
        }

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));

        InvocationResult result = invoker.execute(request);

        if (result.getExecutionException() != null) {
            throw new IllegalStateException("Errore esecuzione Maven: " + result.getExecutionException(), result.getExecutionException());
        }
        if (result.getExitCode() != 0) {
            log.error("Maven terminato con exit code {} per projectKey={}", result.getExitCode(), projectKey);
        }
        return result.getExitCode();
    }

    // progetto senza pom.xml principale
    private int analisiScannerCli(File sourceDir, String projectKey) throws Exception {

        log.info("Nessun pom.xml principale per projectKey={}: analisi con SonarScanner CLI", projectKey);

        List<String> comando = new ArrayList<>();
        comando.add(eseguibileScanner().getAbsolutePath());
        comando.add("-Dsonar.projectKey=" + projectKey);
        comando.add("-Dsonar.projectBaseDir=" + sourceDir.getAbsolutePath());

        for (String parametro : mavenCommandParameterNonJava.split("\\|")) {
            comando.add(parametro);
        }
        if (!sonarScannerParameter.isBlank()) {
            for (String parametro : sonarScannerParameter.split(",")) {
                comando.add(parametro);
            }
        }

        // file .java senza classi compilate
        if (FileUtils.haFileJava(sourceDir.toPath())) {
            FileUtils.creaCartellaBinariVuota(sourceDir.toPath());
            comando.add("-Dsonar.java.binaries=" + FileUtils.CARTELLA_BINARI_VUOTA);
        }

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.directory(sourceDir);
        pb.redirectErrorStream(true);
        pb.environment().put("SONAR_HOST_URL", sonarHostUrl);
        pb.environment().put("SONAR_TOKEN", sonarToken);

        Process processo = pb.start();

        // output dello scanner nel log dell'applicazione
        try (BufferedReader output = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
            String riga;
            while ((riga = output.readLine()) != null) {
                log.info("[sonar-scanner] {}", riga);
            }
        }

        int exitCode = processo.waitFor();
        if (exitCode != 0) {
            log.error("SonarScanner CLI terminato con exit code {} per projectKey={}", exitCode, projectKey);
        }
        return exitCode;
    }

    private File eseguibileScanner() {

        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        File eseguibile = new File(new File(sonarScannerHome, "bin"), windows ? "sonar-scanner.bat" : "sonar-scanner");

        if (!eseguibile.isFile()) {
            throw new IllegalStateException("SonarScanner CLI non trovato: " + eseguibile.getAbsolutePath()
                    + " (controlla sonar.scanner.home)");
        }
        return eseguibile;
    }
}