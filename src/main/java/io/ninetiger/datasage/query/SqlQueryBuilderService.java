package io.ninetiger.datasage.query;

import io.ninetiger.datasage.entity.QueryLog;
import io.ninetiger.datasage.exception.InvalidQueryRequestException;
import io.ninetiger.datasage.exception.QueryProcessingException;
import io.ninetiger.datasage.exception.UnsafeQueryException;
import io.ninetiger.datasage.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
@Slf4j
@RequiredArgsConstructor
public class SqlQueryBuilderService {

    @Qualifier("openAIChatClient")
    private final ChatClient chatClient;
    private final SqlSecurityChecker securityChecker;

    private final QueryLogRepository queryLogRepository;

    @Value("classpath:prompts/sql-system-prompt.txt") Resource systemPromptResource;
    @Value("classpath:prompts/sql-user-prompt.txt") Resource userPromptResource;

    public QueryResult executeNaturalLanguageQuery(JdbcTemplate jdbcTemplate, String question, String schemaInfo, String workspaceId) {
        try {
            // Generate SQL query from natural language
            String sqlQuery = generateSqlQuery(question, schemaInfo);
            
            // Validate and secure the query
            if (!isQuerySafe(sqlQuery)) {
                throw new UnsafeQueryException("Generated query failed security checks");
            }

            QueryLog queryLog = new QueryLog();
            queryLog.setNaturalQuery(question);
            queryLog.setSqlQuery(sqlQuery);
            queryLog.setWorkspaceId(workspaceId);
            queryLogRepository.save(queryLog);

            // Execute the query
            List<Map<String, Object>> results = executeQuery(jdbcTemplate, sqlQuery);
            
            return new QueryResult(sqlQuery, results);
            
        } catch (Exception e) {
            log.error("Error processing natural language query", e);
            throw new QueryProcessingException("Failed to process query", e);
        }
    }

    private String generateSqlQuery(String question, String schemaInfo) {
        // Prepare prompts

        PromptTemplate userPromptTemplate = new PromptTemplate(this.userPromptResource);
        Message userMessage = userPromptTemplate.createMessage(Map.of("question", question, "schema", schemaInfo));

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.systemPromptResource);
        Message systemMessage = systemPromptTemplate.createMessage();

        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));

        // Generate query using AI
        String generatedQuery = chatClient.prompt(prompt).call().content();

        log.info("Generated query: {}", generatedQuery);

        if ("INVALID_QUERY_REQUEST".equals(generatedQuery)) {
            throw new InvalidQueryRequestException("Cannot generate a safe SELECT query for this request");
        }

        return generatedQuery;
    }

    private boolean isQuerySafe(String sqlQuery) {
        return securityChecker.validateQuery(sqlQuery);
    }

    private List<Map<String, Object>> executeQuery(JdbcTemplate jdbcTemplate, String sqlQuery) {
        return jdbcTemplate.queryForList(sqlQuery);
    }
} 