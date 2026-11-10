package com.soldati.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soldati.dto.output.ProjectDTO;
import com.soldati.dto.output.StatoDTO;
import com.soldati.models.Project;
import com.soldati.models.Stato;

public final class Mapper {

    private static final Pattern RELEASE = Pattern.compile("(?i)(?<![a-z])R(\\d+)");
    private static final Pattern SPRINT = Pattern.compile("(?i)(?<![a-z])S(\\d+)");

    private static Long numeroDaTag(Pattern pattern, String tag) {
        Matcher m = pattern.matcher(tag);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    public static Project createProject(String projectName, String tag, Stato statoIniziale) {

        Project project = new Project();
        project.setProjectName(projectName);
        project.setTag(tag);
        project.setDataInserimento(LocalDate.now());
        project.setStato(statoIniziale);

        project.setReleaseNumber(numeroDaTag(RELEASE, tag));
        project.setSprintNumber(numeroDaTag(SPRINT, tag));

        return project;
    }

    public static StatoDTO buildStatoDTO(Stato s) {
        return StatoDTO.builder()
                .statoId(s.getId())
                .descrizione(s.getDescrizione())
                .build();
    }

    public static ProjectDTO buildProgettoDTO(Project project) {
        Stato stato = project.getStato();

        return ProjectDTO.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .tag(project.getTag())
                .releaseNumber(project.getReleaseNumber())
                .sprintNumber(project.getSprintNumber())
                .dataInserimento(project.getDataInserimento())
                .statoId(stato != null ? stato.getId() : null)
                .descrizione(stato != null ? stato.getDescrizione() : null)
                .dataInvioManuali(project.getDataInvioManuali())
                .build();
    }
    
    // riga data dell'ultima analisi, compliance per id project
    public static ProjectDTO buildProgettoAnalizzatoDTO(Object[] riga, Map<Long, BigDecimal> compliance) {
        ProjectDTO p = buildProgettoDTO((Project) riga[0]);
        p.setDataUltimaAnalisi((LocalDateTime) riga[1]);
        p.setCompliance(compliance.get(p.getId()));
        return p;
    }
}
