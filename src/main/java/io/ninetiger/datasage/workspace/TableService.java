package io.ninetiger.datasage.workspace;

import io.ninetiger.datasage.dbconfig.DatabaseConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TableService {

    private final WorkspaceService workspaceService;
    private final DatabaseConfigService databaseConfigService;


}
