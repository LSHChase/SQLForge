package com.company.sqloptimization.application.controller;

import com.company.sqloptimization.application.controller.dto.AccelerationCandidateCreateRequest;
import com.company.sqloptimization.application.controller.vo.AccelerationCandidateVO;
import com.company.sqloptimization.application.service.AccelerationCandidateApplicationService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sql-optimization/acceleration-candidates")
public class AccelerationCandidateController {

    private final AccelerationCandidateApplicationService accelerationCandidateApplicationService;

    public AccelerationCandidateController(AccelerationCandidateApplicationService accelerationCandidateApplicationService) {
        this.accelerationCandidateApplicationService = accelerationCandidateApplicationService;
    }

    @PostMapping
    public AccelerationCandidateVO createCandidate(@Valid @RequestBody AccelerationCandidateCreateRequest request) {
        return accelerationCandidateApplicationService.createCandidate(request);
    }

    @GetMapping
    public List<AccelerationCandidateVO> listCandidates() {
        return accelerationCandidateApplicationService.listCandidates();
    }

    @GetMapping("/{candidateId}")
    public AccelerationCandidateVO getCandidate(@PathVariable("candidateId") String candidateId) {
        return accelerationCandidateApplicationService.getCandidate(candidateId);
    }
}
