package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.AccessParseRequest;
import com.company.sqloptimization.application.controller.dto.CombinedParseRequest;
import com.company.sqloptimization.application.controller.dto.StructureParseRequest;
import com.company.sqloptimization.application.controller.vo.AccessParseResponseVO;
import com.company.sqloptimization.application.controller.vo.CombinedParseStatusVO;
import com.company.sqloptimization.application.controller.vo.StructureParseResponseVO;
import com.company.sqloptimization.application.service.AccessParseApplicationService;
import com.company.sqloptimization.application.service.CombinedParseApplicationService;
import com.company.sqloptimization.application.service.StructureParseApplicationService;
import com.company.sqlforge.common.constants.ErrorCodeConstants;
import com.company.sqlforge.common.exception.BizException;
import org.springframework.http.HttpStatus;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/parse")
public class StructureParseController {

    private final StructureParseApplicationService structureParseApplicationService;
    private final AccessParseApplicationService accessParseApplicationService;
    private final CombinedParseApplicationService combinedParseApplicationService;

    public StructureParseController(StructureParseApplicationService structureParseApplicationService,
                                    AccessParseApplicationService accessParseApplicationService,
                                    CombinedParseApplicationService combinedParseApplicationService) {
        this.structureParseApplicationService = structureParseApplicationService;
        this.accessParseApplicationService = accessParseApplicationService;
        this.combinedParseApplicationService = combinedParseApplicationService;
    }

    @PostMapping("/structure")
    public StructureParseResponseVO parseStructure(@Valid @RequestBody StructureParseRequest request) {
        return structureParseApplicationService.parse(request);
    }

    @PostMapping("/access")
    public AccessParseResponseVO parseAccess(@Valid @RequestBody AccessParseRequest request) {
        return accessParseApplicationService.parseAccess(request, null);
    }

    @PostMapping("/combined")
    public CombinedParseStatusVO parseCombined(@Valid @RequestBody CombinedParseRequest request) {
        return combinedParseApplicationService.submit(request);
    }

    @GetMapping("/{parseTaskId}")
    public CombinedParseStatusVO getCombinedStatus(@PathVariable("parseTaskId") String parseTaskId) {
        CombinedParseStatusVO status = combinedParseApplicationService.getStatus(parseTaskId);
        if (status == null) {
            throw new BizException(
                ErrorCodeConstants.SQL_OPTIMIZATION_TASK_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Combined parse task does not exist for parseTaskId=" + parseTaskId
            );
        }
        return status;
    }
}
