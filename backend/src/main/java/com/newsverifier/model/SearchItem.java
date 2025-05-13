package com.newsverifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchItem {
    private String title;
    private String link;
    private String snippet;
    private String displayLink;
}