package com.soldati.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;

// operazioni su file e cartelle: zip scaricato da GitLab, pom.xml, cartella temporanea dei sorgenti
@Slf4j
public final class FileUtils {

    // prefisso della cartella temporanea in cui GitLabImpl scarica ed estrae i sorgenti
    public static final String PREFISSO_CARTELLA_TEMPORANEA = "sq-source-";

    private static final Pattern CREDENZIALI_SONAR = Pattern.compile("\\s*<sonar\\.(password|login)>[^<]*</sonar\\.\\1>");

    // (analisi con SonarScanner CLI)
    public static final String CARTELLA_BINARI_VUOTA = ".sonar-binari-vuoti";

    private FileUtils() {
    }

    // estrae zip dentro la cartella di destinazione indicata.
    public static void estraiZip(Path fileZip, Path cartellaDestinazione) throws IOException {

        // legge lo zip per file/cartelle
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(fileZip))) {

            ZipEntry entry;

            // Scorriamo file/cartelle una alla volta
            while ((entry = zis.getNextEntry()) != null) {

                // calcola dove sarà estratto, unendo percorso rel. dentro lo zip alla cartella di destinazione
                Path percorsoFile = cartellaDestinazione.resolve(entry.getName()).normalize();

                // impediamo che un file dello zip vada fuori dalla cartella di destinazione
                if (!percorsoFile.startsWith(cartellaDestinazione)) {
                    throw new IOException("Entry ZIP non valida (path traversal): " + entry.getName());
                }

                if (entry.isDirectory()) {

                    // Se l'entry è una cartella la creiamo
                    Files.createDirectories(percorsoFile);
                } else {
                    // Se l'entry è un file copiamo il contenuto del file dallo zip
                    Files.createDirectories(percorsoFile.getParent());
                    Files.copy(zis, percorsoFile, StandardCopyOption.REPLACE_EXISTING);  //se il file esiste lo sovrascrive
                }

                // chiudiamo l'entry per passare alla successiva
                zis.closeEntry();
            }
        }
    }

    // Individua la cartella vera del progetto dentro la cartella estratta
    public static File trovaCartellaProgetto(Path cartellaEstratta) throws IOException {

        // tutto quello che si trova al primo livello della cartella estratta
        try (var elenco = Files.list(cartellaEstratta)) {

            List<Path> elementiPrimoLivello = elenco.toList();

            // Caso 1: la cartella del progetto generata da GitLab, la restituiamo è al primo livello
            if (elementiPrimoLivello.size() == 1 && Files.isDirectory(elementiPrimoLivello.get(0))) {
                return elementiPrimoLivello.get(0).toFile();
            }

            // Caso 2: usiamo direttamente la cartella estratta
            return cartellaEstratta.toFile();
        }
    }

    // toglie sonar.login e sonar.password da tutti i pom.xml della cartella
    public static void rimuoviCredenzialiSonar(Path sourceDir) throws IOException {

        List<Path> pom;
        try (Stream<Path> file = Files.walk(sourceDir)) {
            pom = file.filter(p -> p.getFileName().toString().equals("pom.xml")).toList();
        }

        for (Path p : pom) {
            byte[] originale = Files.readAllBytes(p);

            // il file viene riscritto identico tranne le righe tolte
            String testo = new String(originale, StandardCharsets.ISO_8859_1);
            String pulito = CREDENZIALI_SONAR.matcher(testo).replaceAll("");

            if (!pulito.equals(testo)) {
                Files.write(p, pulito.getBytes(StandardCharsets.ISO_8859_1));
                log.info("Rimosse sonar.login/sonar.password da {} (solo nella copia temporanea)", p);
            }
        }
    }


    // Se manca il plugin maven di Sonar passiamo tutta la cartella come sorgente.
    public static boolean haSorgentiJava(Path sourceDir) throws IOException {
        Path srcMainJava = Path.of("src", "main", "java");
        try (Stream<Path> file = Files.walk(sourceDir)) {
            return file.filter(Files::isDirectory)
                    .filter(p -> p.endsWith(srcMainJava))
                    .filter(p -> !p.toString().contains("node_modules") && !p.toString().contains("target"))
                    .anyMatch(p -> Files.isRegularFile(p.getParent().getParent().getParent().resolve("pom.xml")));
        }
    }

    // cancella la cartella temporanea creata per i sorgenti scaricati
    public static void pulisciCartellaTemporanea(File sourceDir) {

        if (sourceDir == null || !sourceDir.exists()) {
            return;
        }

        // La cartella radice è quella creata da GitLabImpl: contiene source.zip ed extracted.
        Path cartellaRadice = sourceDir.toPath().toAbsolutePath();

        while (cartellaRadice != null && (cartellaRadice.getFileName() == null
                || !cartellaRadice.getFileName().toString().startsWith(PREFISSO_CARTELLA_TEMPORANEA))) {
            cartellaRadice = cartellaRadice.getParent();
        }

        if (cartellaRadice == null) {
            log.warn("Cartella {} non trovata sopra {}: niente da pulire", PREFISSO_CARTELLA_TEMPORANEA, sourceDir);
            return;
        }

        try (Stream<Path> tuttiIFile = Files.walk(cartellaRadice)) {

            // Cancelliamo prima i file più "profondi" e poi le cartelle
            tuttiIFile.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("Impossibile eliminare {}: {}", path, e.getMessage());
                        }
                    });

            log.debug("Directory temporanea ripulita: {}", cartellaRadice);

        } catch (Exception e) {
            log.warn("Errore durante la pulizia della directory temporanea {}: {}", cartellaRadice, e.getMessage());
        }
    }

    public static boolean haPomPrincipale(Path sourceDir) {
        return Files.isRegularFile(sourceDir.resolve("pom.xml"));
    }

    public static boolean haFileJava(Path sourceDir) throws IOException {
        try (Stream<Path> file = Files.walk(sourceDir)) {
            return file.anyMatch(p -> p.getFileName().toString().endsWith(".java"));
        }
    }

    public static void creaCartellaBinariVuota(Path sourceDir) throws IOException {
        Files.createDirectories(sourceDir.resolve(CARTELLA_BINARI_VUOTA));
    }
}
