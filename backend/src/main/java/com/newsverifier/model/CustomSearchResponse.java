package com.newsverifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomSearchResponse {
    private List<SearchItem> items;
    private SearchInformation searchInformation;
} 