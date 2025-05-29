package com.newsverifier.model.search;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleSearchResultItem {
    private String title;
    private String snippet;
    private String link;
}
