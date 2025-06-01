package com.newsverifier.model;

import lombok.Data;

import java.util.List;

@Data
public class NERApiResponseItem {
    private String url;
    private String title;
    private List<NamedEntityResult> entities;
}
