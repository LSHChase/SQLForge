package com.sqlforge.backend.service;

import com.sqlforge.backend.model.ConnectionActivity;
import com.sqlforge.backend.repository.FileConnectionActivityRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ConnectionActivityService {

    private static final int DEFAULT_LIMIT = 20;

    private final FileConnectionActivityRepository activityRepository;

    public ConnectionActivityService(FileConnectionActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public ConnectionActivity record(
        String connectionId,
        String connectionName,
        String actionType,
        String actionStatus,
        String details
    ) {
        return activityRepository.save(
            new ConnectionActivity(
                UUID.randomUUID().toString(),
                connectionId,
                connectionName,
                actionType,
                actionStatus,
                details,
                Instant.now()
            )
        );
    }

    public List<ConnectionActivity> listForConnection(String connectionId) {
        return activityRepository.findByConnectionId(connectionId, DEFAULT_LIMIT);
    }

    public List<ConnectionActivity> listRecentActivity() {
        return activityRepository.findRecent(DEFAULT_LIMIT);
    }
}
