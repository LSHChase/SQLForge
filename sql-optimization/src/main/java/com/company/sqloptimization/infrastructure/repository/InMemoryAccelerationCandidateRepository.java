package com.company.sqloptimization.infrastructure.repository;

import com.company.sqloptimization.domain.candidate.AccelerationCandidate;
import com.company.sqloptimization.domain.candidate.repository.AccelerationCandidateRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
@Primary
public class InMemoryAccelerationCandidateRepository implements AccelerationCandidateRepository {

    private final Map<String, AccelerationCandidate> candidates =
        new ConcurrentHashMap<String, AccelerationCandidate>();

    @Override
    public AccelerationCandidate save(AccelerationCandidate candidate) {
        candidates.put(candidate.getCandidateId(), candidate);
        return candidate;
    }

    @Override
    public AccelerationCandidate findByCandidateId(String candidateId) {
        return candidates.get(candidateId);
    }

    @Override
    public List<AccelerationCandidate> findByTenantId(String tenantId) {
        List<AccelerationCandidate> matches = new ArrayList<AccelerationCandidate>();
        for (AccelerationCandidate candidate : candidates.values()) {
            if (candidate.getTenantId().equals(tenantId)) {
                matches.add(candidate);
            }
        }
        matches.sort(Comparator.comparing(AccelerationCandidate::getCreatedAt).reversed());
        return matches;
    }
}
