package io.ninetiger.datasage.dbconfig;

public record DatabaseConfig(
        String name,
        String engine,
        String host,
        int port,
        String user,
        String password,
        String database,
        String schema
) {


}
