package com.soldati.mapper;

import java.util.Map;

import com.soldati.dto.output.GitLabProjectDTO;

public final class GitLabMapper {

    private GitLabMapper() {
    }

    // progetto restituito dalle API GitLab -> DTO con id, nome e path
    public static GitLabProjectDTO buildGitLabProjectDTO(Map<String, Object> project) {

        Object id = project.get("id");
        Object name = project.get("name");
        Object pathWithNamespace = project.get("path_with_namespace");

        GitLabProjectDTO gitLab = new GitLabProjectDTO();
        gitLab.setId(id != null ? Long.valueOf(id.toString()) : null);
        gitLab.setName(name != null ? name.toString() : null);
        gitLab.setPathWithNamespace(pathWithNamespace != null ? pathWithNamespace.toString() : null);

        return gitLab;
    }
}
