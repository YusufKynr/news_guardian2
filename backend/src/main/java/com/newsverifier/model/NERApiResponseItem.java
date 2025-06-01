package com.newsverifier.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class NERApiResponseItem {
    private String text;
    private List<NamedEntityResult> entities;
}
