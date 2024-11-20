package io.ninetiger.datasage.workspace;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ninetiger.datasage.dbconfig.DatabaseConfig;
import io.ninetiger.datasage.dbconfig.DatabaseConfigService;
import io.ninetiger.datasage.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableMetadataService {

    private final WorkspaceService workspaceService;
    private final DatabaseConfigService databaseConfigService;

    private Map<String,String> schemaInfo = new ConcurrentHashMap<>();

    Map<String, String> jdbcUrlTemplateMap = Map.of("postgresql", "jdbc:postgresql://%s:%d/%s"
    , "mysql", "jdbc:mysql://%s:%d/%s"
            ,"mariadb", "jdbc:mariadb://%s:%d/%s"
            ,"sqlserver", "jdbc:sqlserver://%s:%d;databaseName=%s"
        ,"oracle", "jdbc:oracle:thin:@%s:%d:%s");

    public JdbcTemplate createJdbcTemplate(String workspaceId) {

        Workspace workspace = this.workspaceService.findById(workspaceId);

        DatabaseConfig config = databaseConfigService.findByName(workspace.name());

        DataSource dataSource = buildDataSource(config);

        return new JdbcTemplate(dataSource);
    }

    public String generateJdbcUrl(String dbType, String ip, int port, String database, String schema) {

        // Validate database type
        if (!jdbcUrlTemplateMap.containsKey(dbType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }

        // Format the base JDBC URL
        String jdbcUrl = String.format(jdbcUrlTemplateMap.get(dbType.toLowerCase()), ip, port, database);

        // Append schema for supported databases
        if (schema != null && !schema.isEmpty()) {
            switch (dbType.toLowerCase()) {
                case "postgresql":
                case "mysql":
                case "mariadb":
                    jdbcUrl += "?currentSchema=" + schema;
                    break;
                case "sqlserver":
                    jdbcUrl += ";schema=" + schema;
                    break;
                case "oracle":
                    // Schema is handled through other means (e.g., user configuration) in Oracle, ignore it
                    break;
                default:
                    break;
            }
        }

        return jdbcUrl;
    }

    private DataSource buildDataSource(DatabaseConfig dbConfig) {
        final HikariConfig config = new HikariConfig();

        config.setJdbcUrl(generateJdbcUrl(
                dbConfig.engine()
                ,dbConfig.host()
                ,dbConfig.port()
                ,dbConfig.database()
                ,dbConfig.schema()
        ));
        config.setUsername(dbConfig.user());
        config.setPassword(dbConfig.password());
        config.setAutoCommit(false);
        config.setMaximumPoolSize(1);

        return new HikariDataSource(config);
    }

    /**
     * Get metadata for all tables in the specified db config
     */
    public List<TableMetadata> getAllTablesMetadata(String workspaceId) {
        try {
            DatabaseMetaData databaseMetaData = createJdbcTemplate(workspaceId).getDataSource().getConnection().getMetaData();
            List<TableMetadata> tables = new ArrayList<>();
            ResultSet tablesRs = databaseMetaData.getTables(
                null, // catalog
                null,
                null, // tableNamePattern
                new String[]{"TABLE"} // types
            );

            while (tablesRs.next()) {
                String tableName = tablesRs.getString("TABLE_NAME");
                String tableComment = tablesRs.getString("REMARKS");
                String schemaName = tablesRs.getString("TABLE_SCHEM");

                log.info("Schema name - {}", schemaName);
                
                TableMetadata tableMetadata = new TableMetadata();
                tableMetadata.setName(tableName);
                tableMetadata.setComment(tableComment);
                tableMetadata.setColumns(getColumnsMetadata(databaseMetaData, schemaName, tableName));
                tableMetadata.setPrimaryKeys(getPrimaryKeys(databaseMetaData, schemaName, tableName));
                tableMetadata.setForeignKeys(getForeignKeys(databaseMetaData, schemaName, tableName));
                tableMetadata.setIndexes(getIndexes(databaseMetaData, schemaName, tableName));
                
                tables.add(tableMetadata);
            }
            
            return tables;
        } catch (SQLException e) {
            log.error("Error retrieving table metadata", e);
            throw new RuntimeException("Failed to retrieve table metadata", e);
        }
    }

    /**
     * Get metadata for columns of a specific table
     */
    private List<ColumnMetadata> getColumnsMetadata(DatabaseMetaData databaseMetaData, String schemaName, String tableName) throws SQLException {
        List<ColumnMetadata> columns = new ArrayList<>();
        ResultSet columnsRs = databaseMetaData.getColumns(
            null, // catalog
            schemaName,
            tableName,
            null // columnNamePattern
        );

        while (columnsRs.next()) {
            ColumnMetadata column = new ColumnMetadata();
            column.setName(columnsRs.getString("COLUMN_NAME"));
            column.setType(columnsRs.getString("TYPE_NAME"));
            column.setSize(columnsRs.getInt("COLUMN_SIZE"));
            column.setNullable(columnsRs.getBoolean("NULLABLE"));
            column.setPosition(columnsRs.getInt("ORDINAL_POSITION"));
            column.setComment(columnsRs.getString("REMARKS"));
            column.setDefaultValue(columnsRs.getString("COLUMN_DEF"));
            
            columns.add(column);
        }
        
        return columns;
    }

    /**
     * Get primary key information for a table
     */
    private List<String> getPrimaryKeys(DatabaseMetaData databaseMetaData, String schemaName, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        ResultSet pkRs = databaseMetaData.getPrimaryKeys(null, schemaName, tableName);
        
        while (pkRs.next()) {
            primaryKeys.add(pkRs.getString("COLUMN_NAME"));
        }
        
        return primaryKeys;
    }

    /**
     * Get foreign key information for a table
     */
    private List<ForeignKeyMetadata> getForeignKeys(
            DatabaseMetaData databaseMetaData,
            String schemaName, String tableName) throws SQLException {
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        ResultSet fkRs = databaseMetaData.getImportedKeys(null, schemaName, tableName);
        
        while (fkRs.next()) {
            ForeignKeyMetadata fk = new ForeignKeyMetadata();
            fk.setName(fkRs.getString("FK_NAME"));
            fk.setColumnName(fkRs.getString("FKCOLUMN_NAME"));
            fk.setReferenceTable(fkRs.getString("PKTABLE_NAME"));
            fk.setReferenceColumn(fkRs.getString("PKCOLUMN_NAME"));
            
            foreignKeys.add(fk);
        }
        
        return foreignKeys;
    }

    /**
     * Get index information for a table
     */
    private List<IndexMetadata> getIndexes(
            DatabaseMetaData databaseMetaData,
            String schemaName, String tableName) throws SQLException {
        Map<String, IndexMetadata> indexMap = new HashMap<>();
        ResultSet indexRs = databaseMetaData.getIndexInfo(null, schemaName, tableName, false, false);

        try {
            while (indexRs.next()) {
                String indexName = indexRs.getString("INDEX_NAME");
                if (indexName == null) continue; // Skip table statistics

                IndexMetadata index = indexMap.computeIfAbsent(indexName, k -> {
                    IndexMetadata idx = new IndexMetadata();
                    idx.setName(indexName);
                    //idx.setUnique(!indexRs.getBoolean("NON_UNIQUE"));
                    idx.setColumns(new ArrayList<>());
                    return idx;
                });

                index.getColumns().add(indexRs.getString("COLUMN_NAME"));
            }
        }catch (SQLException se) {
            throw new RuntimeException("Failed to retrieve index metadata", se);
        }
        
        return new ArrayList<>(indexMap.values());
    }

    public String getWorkspaceSchemaInfo(String workspaceId) {

        if(this.schemaInfo.containsKey(workspaceId)) {
            return this.schemaInfo.get(workspaceId);
        }

        List<TableMetadata> tableMetadata = getAllTablesMetadata(workspaceId);

        String schemaInfo = formatSchemaInfoForLLM(tableMetadata);

        this.schemaInfo.put(workspaceId, schemaInfo);

        return schemaInfo;
    }

    private String formatSchemaInfoForLLM(List<TableMetadata> tables) {
        StringBuilder schema = new StringBuilder();
        schema.append("Database Schema Definition:\n\n");

        // First, list all tables with their descriptions
        schema.append("Available Tables:\n");
        for (TableMetadata table : tables) {
            schema.append("- ").append(table.getName());
            if (table.getComment() != null) {
                schema.append(" (").append(table.getComment()).append(")");
            }
            schema.append("\n");
        }
        schema.append("\n");

        // Then, provide detailed information for each table
        for (TableMetadata table : tables) {
            schema.append("Table: ").append(table.getName()).append("\n");
            if (table.getComment() != null) {
                schema.append("Description: ").append(table.getComment()).append("\n");
            }

            // Primary Keys
            if (!table.getPrimaryKeys().isEmpty()) {
                schema.append("Primary Key(s): ")
                        .append(String.join(", ", table.getPrimaryKeys()))
                        .append("\n");
            }

            // Columns with their details
            schema.append("Columns:\n");
            for (ColumnMetadata column : table.getColumns()) {
                schema.append("  * ").append(column.getName())
                        .append(" (").append(column.getType());

                if (column.getSize() != null) {
                    schema.append("(").append(column.getSize()).append(")");
                }
                schema.append(")");

                List<String> constraints = new ArrayList<>();
                if (!column.getNullable()) {
                    constraints.add("NOT NULL");
                }
                if (table.getPrimaryKeys().contains(column.getName())) {
                    constraints.add("PRIMARY KEY");
                }

                if (!constraints.isEmpty()) {
                    schema.append(" [").append(String.join(", ", constraints)).append("]");
                }

                if (column.getComment() != null) {
                    schema.append(" - ").append(column.getComment());
                }
                schema.append("\n");
            }

            // Foreign Key Relationships
            if (!table.getForeignKeys().isEmpty()) {
                schema.append("Relationships:\n");
                for (ForeignKeyMetadata fk : table.getForeignKeys()) {
                    schema.append("  * ")
                            .append(table.getName()).append(".").append(fk.getColumnName())
                            .append(" → ")
                            .append(fk.getReferenceTable()).append(".").append(fk.getReferenceColumn())
                            .append("\n");
                }
            }
            schema.append("\n");
        }



        return schema.toString();
    }

}