package com.newsverifier.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NamedEntityResult {
    private String entity;
    private String word;
}
