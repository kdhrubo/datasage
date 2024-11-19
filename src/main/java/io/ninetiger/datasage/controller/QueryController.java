package io.ninetiger.datasage.controller;

import io.ninetiger.datasage.exception.InvalidQueryRequestException;
import io.ninetiger.datasage.exception.QueryProcessingException;
import io.ninetiger.datasage.exception.UnsafeQueryException;
import io.ninetiger.datasage.query.QueryResult;
import io.ninetiger.datasage.query.SqlQueryBuilderService;
import io.ninetiger.datasage.workspace.TableMetadataService;
import io.ninetiger.datasage.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/query")
@Slf4j
@RequiredArgsConstructor
public class QueryController {

    private final SqlQueryBuilderService queryBuilderService;
    private final TableMetadataService tableMetadataService;

    @PostMapping("/{workspaceId}")
    public String executeNaturalLanguageQuery(
            @RequestParam String question,
            @PathVariable String workspaceId, Model model) {
        try {

            log.info("Question asked : {}", question);
            log.info("Workspace asked : {}", workspaceId);

            String schemaInfo = tableMetadataService.getWorkspaceSchemaInfo(workspaceId);
            if (schemaInfo == null) {
                model.addAttribute("error", "Workspace schema not found");
                return "fragments/query-result :: error";
            }

            JdbcTemplate jdbcTemplate = tableMetadataService.createJdbcTemplate(workspaceId);

            QueryResult result = queryBuilderService.executeNaturalLanguageQuery(jdbcTemplate, question, schemaInfo);

            model.addAttribute("sqlQuery", result.sqlQuery());
            model.addAttribute("results", result.results());

            if (result.results().isEmpty()) {
                return "fragments/query-result :: no-results";
            }

            return "fragments/query-result :: results";

        } catch (UnsafeQueryException | InvalidQueryRequestException e) {
            log.warn("Query rejected for workspace {}: {}", workspaceId, e.getMessage());
            model.addAttribute("error", "Invalid query: " + e.getMessage());
            return "fragments/query-result :: error";
        } catch (QueryProcessingException e) {
            log.error("Query processing error for workspace {}", workspaceId, e);
            model.addAttribute("error", "Error processing query");
            return "fragments/query-result :: error";
        }
    }
} 