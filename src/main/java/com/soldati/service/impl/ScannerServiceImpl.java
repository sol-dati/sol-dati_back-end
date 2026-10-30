package com.soldati.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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

import com.soldati.service.interfaces.ScannerService;
import com.soldati.util.FileUtils;

import lombok.extern.slf4j.Slf4j;

// Analisi Sonar con lo scanner adatto al progetto:
// - pom.xml + codice Java in un modulo Maven -> build Maven con la prima JDK che funziona, poi SonarScanner for Maven
// - tutti gli altri progetti                 -> SonarScanner CLI (analizza la cartella così com'è)
@Slf4j
@Service
public class ScannerServiceImpl implements ScannerService {

    // dove è installato maven sul server
    @Value("${maven.home}")
    private String mavenHome;

    // passo 1: parametri e goal del build (separati da virgola)
    @Value("${maven.build.parameter}")
    private String mavenBuildParameter;

    // passo 2: parametri e goal dell'analisi Sonar (separati da virgola, "?" = projectKey)
    @Value("${maven.sonar.parameter}")
    private String mavenSonarParameter;

    // sorgenti ed esclusioni per i progetti analizzati con la CLI (separati da "|")
    @Value("${maven.command.parameter.non.java}")
    private String mavenCommandParameterNonJava;

    // dove è installato SonarScanner CLI sul server (la cartella che contiene bin/)
    @Value("${sonar.scanner.home}")
    private String sonarScannerHome;

    // parametri aggiuntivi per SonarScanner CLI, separati da virgola
    @Value("${sonar.scanner.parameter:}")
    private String sonarScannerParameter;

    // server e token sonar: alla CLI arrivano come variabili d'ambiente, non sulla riga di comando
    @Value("${sonar.host.url}")
    private String sonarHostUrl;

    @Value("${sonar.token}")
    private String sonarToken;

    // JDK da provare in ordine per il build (in fondo si aggiunge sempre la Java del backend)
    @Value("${sonar.jdk.compilazione:}")
    private List<String> jdkCompilazione;

    @Override
    public int runSonarAnalisi(File sourceDir, String projectName, String projectKey) throws Exception {

        if (sourceDir == null || !sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IllegalArgumentException("sourceDir non valida: " + sourceDir);
        }
        if (projectKey == null || projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey non può essere vuota");
        }

        Path cartella = sourceDir.toPath();
        // Maven serve solo se c'è codice Java da compilare; tutto il resto va con la CLI
        boolean progettoJavaMaven = FileUtils.haPomPrincipale(cartella) && FileUtils.haSorgentiJava(cartella);

        int exitCode = progettoJavaMaven
                ? analisiMaven(sourceDir, projectName, projectKey)
                : analisiScannerCli(sourceDir, projectKey);

        if (exitCode != 0) {
            throw new IllegalStateException("Analisi Sonar terminata con exit code: " + exitCode + " per projectKey=" + projectKey);
        }
        log.debug("Analisi Sonar completata con successo per projectKey={}", projectKey);

        return exitCode;
    }

    // progetto Maven: build con la prima JDK che funziona, poi analisi Sonar con la JDK del backend
    private int analisiMaven(File sourceDir, String projectName, String projectKey) throws Exception {

        log.debug("Avvio analisi Maven/Sonar per sourceDir={}, projectKey={}", sourceDir, projectKey);

        // i pom con sonar.password/sonar.login fanno fallire l'analisi: li tolgo dalla copia scaricata
        FileUtils.rimuoviCredenzialiSonar(sourceDir.toPath());

        // passo 1: build, provando le JDK in ordine finché una riesce
        File jdkBuild = null;
        int exitCode = -1;
        for (File jdk : jdkCandidate()) {
            log.info("Build Maven di {} con la JDK {}", projectName, jdk);
            exitCode = eseguiMaven(sourceDir, jdk, List.of(mavenBuildParameter.split(",")));
            if (exitCode == 0) {
                jdkBuild = jdk;
                break;
            }
            log.warn("Build di {} fallita con la JDK {}: provo con la successiva", projectName, jdk);
        }
        if (jdkBuild == null) {
            log.error("Build Maven fallita con tutte le JDK per projectKey={}", projectKey);
            return exitCode;
        }

        // passo 2: analisi Sonar sulle classi già compilate (lo scanner richiede una Java recente)
        List<String> parametriSonar = new ArrayList<>();
        for (String parametro : mavenSonarParameter.split(",")) {
            parametriSonar.add(parametro.replace("?", "-Dsonar.projectKey=" + projectKey));
        }
        // JDK con cui è stato compilato il progetto, per risolvere correttamente le classi Java standard
        parametriSonar.add("-Dsonar.java.jdkHome=" + jdkBuild.getAbsolutePath());

        File jdkBackend = new File(System.getProperty("java.home"));
        log.info("Analisi Sonar di {} con la JDK {}", projectName, jdkBackend);
        exitCode = eseguiMaven(sourceDir, jdkBackend, parametriSonar);
        if (exitCode != 0) {
            log.error("Analisi Sonar fallita con exit code {} per projectKey={}", exitCode, projectKey);
        }
        return exitCode;
    }

    // JDK da provare per il build: quelle configurate, in ordine, e per ultima la Java del backend
    private List<File> jdkCandidate() {

        List<File> candidate = new ArrayList<>();
        for (String percorso : jdkCompilazione) {
            File jdk = new File(percorso.trim());
            if (!jdk.isDirectory()) {
                throw new IllegalStateException("JDK non trovata: " + percorso + " (controlla sonar.jdk.compilazione)");
            }
            candidate.add(jdk);
        }
        candidate.add(new File(System.getProperty("java.home")));
        return candidate;
    }

    // esegue Maven nella cartella del progetto con la JDK e i parametri indicati
    private int eseguiMaven(File sourceDir, File javaHome, List<String> parametri) throws Exception {

        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(sourceDir);   // cartella dei sorgenti scaricati
        request.setBatchMode(true);            // esecuzione senza interazione
        request.setJavaHome(javaHome);         // mai la Java trovata nel PATH
        parametri.forEach(request::addArg);

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(mavenHome));

        InvocationResult result = invoker.execute(request);

        if (result.getExecutionException() != null) {
            throw new IllegalStateException("Errore esecuzione Maven: " + result.getExecutionException(), result.getExecutionException());
        }
        return result.getExitCode();
    }

    // progetto senza pom.xml o senza codice Java: SonarScanner CLI sulla cartella
    private int analisiScannerCli(File sourceDir, String projectKey) throws Exception {

        log.info("Progetto senza pom.xml o senza codice Java per projectKey={}: analisi con SonarScanner CLI", projectKey);

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

        // file .java senza classi compilate: Sonar richiede sonar.java.binaries, gli passo una cartella vuota
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

    // bin/sonar-scanner.bat su Windows, bin/sonar-scanner sugli altri sistemi
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