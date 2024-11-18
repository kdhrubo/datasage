package io.ninetiger.datasage.workspace;

import lombok.Data;

@Data
public class ColumnMetadata {
    private String name;
    private String type;
    private Integer size;
    private Boolean nullable;
    private Integer position;
    private String comment;
    private String defaultValue;
}