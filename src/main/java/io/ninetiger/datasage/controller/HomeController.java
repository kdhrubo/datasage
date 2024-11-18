package io.ninetiger.datasage.controller;

import io.ninetiger.datasage.dbconfig.DatabaseConfigService;
import io.ninetiger.datasage.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class HomeController {


    private final WorkspaceService workspaceService;
    private final DatabaseConfigService databaseConfigService;


    @GetMapping({"", "/"})
    public String showIndex(Model model) {
        model.addAttribute(
                "workspaces",
                workspaceService.getWorkspaces());
        return "index";
    }

}
