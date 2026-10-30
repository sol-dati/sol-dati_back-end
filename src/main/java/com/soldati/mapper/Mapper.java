package com.soldati.mapper;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.soldati.dto.input.SogliaReq;
import com.soldati.dto.input.TipoInserimentoReq;
import com.soldati.dto.output.ProjectDTO;
import com.soldati.dto.output.SogliaDTO;
import com.soldati.dto.output.StatoDTO;
import com.soldati.dto.output.TipoInserimentoDTO;
import com.soldati.models.Project;
import com.soldati.models.Soglia;
import com.soldati.models.Stato;
import com.soldati.models.TipoInserimento;

public final class Mapper {

    private static final Pattern RELEASE = Pattern.compile("(?i)(?<![a-z])R(\\d+)");
    private static final Pattern SPRINT = Pattern.compile("(?i)(?<![a-z])S(\\d+)");

    private Mapper() {
    }

    private static Long numeroDaTag(Pattern pattern, String tag) {
        Matcher m = pattern.matcher(tag);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    public static Project createProject(String projectName, String tag) {

        Project project = new Project();
        project.setProjectName(projectName);
        project.setTag(tag);
        project.setDataInserimento(LocalDate.now());

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
                .build();
    }

    public static SogliaDTO buildSogliaDTO(Soglia s) {
        return SogliaDTO.builder()
                .idSoglia(s.getIdSoglia())
                .descrizione(s.getDescrizione())
                .build();
    }

    public static TipoInserimentoDTO buildTipoInserimentoDTO(TipoInserimento t) {
        return TipoInserimentoDTO.builder()
                .idTipo(t.getIdTipo())
                .descrizione(t.getDescrizione())
                .build();
    }

    public static SogliaReq createSoglia(String descrizione) {
        SogliaReq req = new SogliaReq();
        req.setDescrizione(descrizione);
        return req;
    }

    public static TipoInserimentoReq createTipoInserimento(String descrizione) {
        TipoInserimentoReq req = new TipoInserimentoReq();
        req.setDescrizione(descrizione);
        return req;
    }
}
