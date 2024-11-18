package io.ninetiger.datasage.dbconfig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseConfigService {

    @Value("classpath:/db.json")
    private Resource resource;

    @Getter
    private List<DatabaseConfig> databases;

    public DatabaseConfig findByName(String name) {
        return
        databases.stream()
                .filter(databaseConfig -> databaseConfig.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

    }


    @PostConstruct
    public void load() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            TypeReference<List<DatabaseConfig>> typeReference = new TypeReference<List<DatabaseConfig>>(){};

            databases = mapper.readValue(resource.getInputStream(),typeReference);

            log.info("Database configs: {}", databases);
        } catch (IOException e){
            log.error(e.getMessage());
        }

    }


}
