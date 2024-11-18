package io.ninetiger.datasage.workspace;

import lombok.Data;

import java.util.List;

@Data
public class TableMetadata {
    private String name;
    private String comment;
    private List<ColumnMetadata> columns;
    private List<String> primaryKeys;
    private List<ForeignKeyMetadata> foreignKeys;
    private List<IndexMetadata> indexes;
}