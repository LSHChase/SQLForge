package com.company.sqloptimization.domain.candidate.repository;

import com.company.sqloptimization.domain.candidate.AccelerationCandidate;
import java.util.List;

public interface AccelerationCandidateRepository {

    AccelerationCandidate save(AccelerationCandidate candidate);

    AccelerationCandidate findByCandidateId(String candidateId);

    List<AccelerationCandidate> findByTenantId(String tenantId);
}
