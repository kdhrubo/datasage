package io.ninetiger.datasage.workspace;

import lombok.Data;

@Data
public class ForeignKeyMetadata {
    private String name;
    private String columnName;
    private String referenceTable;
    private String referenceColumn;
}