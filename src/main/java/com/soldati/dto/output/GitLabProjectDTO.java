package com.soldati.dto.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GitLabProjectDTO {

    private Long id;
    private String name;
    private String pathWithNamespace;
}
