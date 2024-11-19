package io.ninetiger.datasage.query;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Slf4j
public class SqlSecurityChecker {

    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "^\\s*SELECT\\s+(?!\\*).+?\\s+FROM\\s+", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Set<String> FORBIDDEN_KEYWORDS = new HashSet<>(Arrays.asList(
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
        "GRANT", "REVOKE", "MERGE", "UPSERT", "EXECUTE", "EXEC",
        "UNION", "INTERSECT", "EXCEPT", "INTO", "PROCEDURE", "FUNCTION"
    ));

    private static final Set<String> DANGEROUS_PATTERNS = new HashSet<>(Arrays.asList(
        "--", "/*", "*/", "@@", "WAITFOR", "DELAY", "SLEEP",
        "XP_", "SP_", "0x", "CHAR(", "NCHAR(", "VARCHAR(", "NVARCHAR(",
        "DECLARE", "CAST", "CONVERT", "CURSOR", "FETCH",
        "EXEC", "EXECUTE", "OPEN", "CLOSE", "DEALLOCATE"
    ));

    @Value("${sql.security.max-query-length:4000}")
    private int maxQueryLength;

    public boolean validateQuery(String sqlQuery) {
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            log.warn("Query rejected: Empty query");
            return false;
        }

        // Check query length
        if (sqlQuery.length() > maxQueryLength) {
            log.warn("Query rejected: Exceeds maximum length of {}", maxQueryLength);
            return false;
        }

        String normalizedQuery = sqlQuery.trim().toUpperCase();

        // Check if it's a SELECT query
        if (!SELECT_PATTERN.matcher(normalizedQuery).find()) {
            log.warn("Query rejected: Not a valid SELECT query");
            return false;
        }

        // Check for forbidden keywords
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (containsWord(normalizedQuery, keyword)) {
                log.warn("Query rejected: Contains forbidden keyword: {}", keyword);
                return false;
            }
        }

        // Check for dangerous patterns
        for (String pattern : DANGEROUS_PATTERNS) {
            if (normalizedQuery.contains(pattern)) {
                log.warn("Query rejected: Contains dangerous pattern: {}", pattern);
                return false;
            }
        }

        // Check for multiple statements
        if (containsMultipleStatements(normalizedQuery)) {
            log.warn("Query rejected: Multiple statements not allowed");
            return false;
        }

        // Check for comment indicators
        if (containsComments(normalizedQuery)) {
            log.warn("Query rejected: Comments not allowed");
            return false;
        }

        // Check for typical SQL injection patterns
        if (containsSqlInjectionPatterns(normalizedQuery)) {
            log.warn("Query rejected: Potential SQL injection patterns detected");
            return false;
        }

        return true;
    }

    private boolean containsSqlInjectionPatterns(String query) {
        // Common SQL injection patterns
        String[] attackPatterns = {
            "'\\s+OR\\s+'1'\\s*=\\s*'1",    // OR-based injection
            "'\\s+OR\\s+1\\s*=\\s*1",       // Numeric OR-based injection
            "'\\s+AND\\s+'1'\\s*=\\s*'1",   // AND-based injection
            "'\\s+AND\\s+1\\s*=\\s*1",      // Numeric AND-based injection
            "';\\s*SELECT\\s+",             // Piggybacked query
            "';\\s*WAITFOR\\s+DELAY\\s+'",  // Time-based injection
            "BENCHMARK\\s*\\(",             // MySQL benchmark attack
            "SLEEP\\s*\\(",                 // MySQL sleep attack
            "INFORMATION_SCHEMA\\.",        // Information schema access
            "LOAD_FILE\\s*\\(",            // File access attempt
            "INTO\\s+OUTFILE\\s+",         // File write attempt
            "INTO\\s+DUMPFILE\\s+"         // File dump attempt
        };

        for (String pattern : attackPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(query).find()) {
                log.warn("SQL injection pattern detected: {}", pattern);
                return true;
            }
        }

        return false;
    }

    private boolean containsMultipleStatements(String query) {
        // Check for semicolons that are not within quotes
        boolean inQuote = false;
        int len = query.length();
        
        for (int i = 0; i < len; i++) {
            char c = query.charAt(i);
            
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (c == ';' && !inQuote) {
                return true;
            }
        }
        
        return false;
    }

    private boolean containsComments(String query) {
        // Check for comments that are not within quotes
        boolean inQuote = false;
        int len = query.length();
        
        for (int i = 0; i < len - 1; i++) {
            char c = query.charAt(i);
            char next = query.charAt(i + 1);
            
            if (c == '\'') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if ((c == '-' && next == '-') || 
                    (c == '/' && next == '*') || 
                    (c == '*' && next == '/')) {
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean containsWord(String text, String word) {
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }
}