package com.newsverifier.model.search;

import lombok.Data;
import java.util.List;

@Data
public class GoogleSearchResultList {
    private List<GoogleSearchResultItem> items;
}
