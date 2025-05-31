package com.newsverifier.model.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class GoogleSearchResultList {
    private List<GoogleSearchResultItem> items;
}
