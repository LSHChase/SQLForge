package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.service.StructureParseApplicationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse")
public class StructureParseController {

    private final StructureParseApplicationService structureParseApplicationService;

    public StructureParseController(StructureParseApplicationService structureParseApplicationService) {
        this.structureParseApplicationService = structureParseApplicationService;
    }

    @PostMapping("/structure")
    public StructureParseResponseVO parseStructure(@Valid @RequestBody StructureParseRequest request) {
        return structureParseApplicationService.parse(request);
    }
}
