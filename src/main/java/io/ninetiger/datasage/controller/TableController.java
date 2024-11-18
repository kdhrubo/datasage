package io.ninetiger.datasage.controller;

import io.ninetiger.datasage.workspace.TableMetadataService;
import io.ninetiger.datasage.workspace.TableService;
import io.ninetiger.datasage.workspace.Workspace;
import io.ninetiger.datasage.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Controller
@RequestMapping("/workspaces/tables")
public class TableController {

    private final TableMetadataService tableMetadataService;
    private final WorkspaceService workspaceService;

    @GetMapping("/{workspaceId}")
    public String getWorkspaceTables(@PathVariable String workspaceId, Model model) {

        log.info("get workspace tables for {}", workspaceId);

        Workspace workspace = workspaceService.findById(workspaceId);

        model.addAttribute("workspaceName", workspace.name());
        model.addAttribute("tables", tableMetadataService.getAllTablesMetadata(workspaceId));
        return "fragments/tables-offcanvas :: tables-offcanvas";
    }
} 