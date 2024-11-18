package io.ninetiger.datasage.workspace;

import lombok.Data;

import java.util.List;

@Data
public class IndexMetadata {
    private String name;
    private boolean unique;
    private List<String> columns;
}