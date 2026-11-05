package com.soldati.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.soldati.dto.output.ProjectDTO;
import com.soldati.exception.SoldatiException;
import com.soldati.mapper.Mapper;
import com.soldati.models.Project;
import com.soldati.repository.ProjectRepo;
import com.soldati.service.interfaces.ComplianceService;
import com.soldati.service.interfaces.GitLabService;
import com.soldati.service.interfaces.ProjectService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProjectImpl implements ProjectService {

    private final ProjectRepo repo;
    private final GitLabService gitLabService;
    private final ComplianceService complianceService;

    @Override
    public int sincronizzaDaGitLab() throws SoldatiException {

        List<String> projectNames = gitLabService.getProjectNames();

        int nuove = 0;

        for (String nome : projectNames) {
            try {
                nuove += create(nome);
            } catch (Exception e) {
                log.warn("Sync GitLab fallita per il progetto {}: {}", nome, e.getMessage());
            }
        }

        return nuove;
    }

    private int create(String projectName) throws SoldatiException {

        List<String> tagGitLab = gitLabService.getTags(projectName);
        Set<String> tagPresenti = repo.findTagSetByProjectName(projectName);

        List<Project> newRec = new ArrayList<>();

        for (String tag : tagGitLab) {
            if (tag == null || tag.isBlank() || tagPresenti.contains(tag)) {
                continue;
            }
            newRec.add(Mapper.createProject(projectName, tag));
            tagPresenti.add(tag);
        }

        repo.saveAll(newRec);

        return newRec.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> findAll(String projectName) throws SoldatiException {

        List<Project> righe = (projectName == null || projectName.isBlank())
                ? repo.findAll()
                : repo.findByProjectNameOrderByTagDesc(projectName.trim().toLowerCase(Locale.ROOT));

        return righe.stream()
                .map(Mapper::buildProgettoDTO)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> findAnalizzati() {

        Map<Long, BigDecimal> compliance = complianceService.compliancePerTag();

        return repo.findAnalizzati().stream()
                .map(riga -> Mapper.buildProgettoAnalizzatoDTO(riga, compliance))
                .toList();
    }
}
