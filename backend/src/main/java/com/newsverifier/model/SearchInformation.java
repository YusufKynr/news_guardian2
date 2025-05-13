package com.newsverifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchInformation {
    private String searchTime;
    private String totalResults;
    private String formattedSearchTime;
    private String formattedTotalResults;
}