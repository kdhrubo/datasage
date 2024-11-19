package io.ninetiger.datasage.query;

import java.util.List;
import java.util.Map;

public record QueryResult (String sqlQuery,
    List<Map<String, Object>> results)
    {}