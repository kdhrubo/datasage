package io.ninetiger.datasage.workspace;

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
public class WorkspaceService {

    @Value("classpath:/workspace.json")
    private Resource resource;

    @Getter
    private List<Workspace> workspaces;


    public Workspace findById(String id) {
        return
        workspaces.stream()
                .filter(workspace -> workspace.id().equals(id))
                .findFirst().get();
    }

    @PostConstruct
    public void load() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            TypeReference<List<Workspace>> typeReference = new TypeReference<List<Workspace>>(){};

            workspaces = mapper.readValue(resource.getInputStream(),typeReference);

            log.info("workspace configs: {}", workspaces);
        } catch (IOException e){
            log.error(e.getMessage());
        }

    }



}
