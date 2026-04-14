package com.sqlforge.backend.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlforge.backend.model.ConnectionActivity;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class FileConnectionActivityRepository {

    private final ObjectMapper objectMapper;
    private final String storageFilePath;
    private final List<ConnectionActivity> activities = new ArrayList<ConnectionActivity>();

    public FileConnectionActivityRepository(
        ObjectMapper objectMapper,
        @Value("${sqlforge.storage.activity-file:${java.io.tmpdir}/sqlforge/connection-activity.json}") String storageFilePath
    ) {
        this.objectMapper = objectMapper;
        this.storageFilePath = storageFilePath;
    }

    @PostConstruct
    public synchronized void load() {
        File storageFile = new File(storageFilePath);

        if (!storageFile.exists()) {
            return;
        }

        try {
            List<ConnectionActivity> persisted = objectMapper.readValue(
                storageFile,
                new TypeReference<List<ConnectionActivity>>() { }
            );
            activities.clear();
            activities.addAll(persisted);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to read persisted connection activities", exception);
        }
    }

    public synchronized ConnectionActivity save(ConnectionActivity activity) {
        activities.add(activity);
        persist();
        return activity;
    }

    public synchronized List<ConnectionActivity> findByConnectionId(String connectionId, int limit) {
        List<ConnectionActivity> matched = new ArrayList<ConnectionActivity>();

        for (int index = activities.size() - 1; index >= 0; index -= 1) {
            ConnectionActivity activity = activities.get(index);

            if (activity.getConnectionId().equals(connectionId)) {
                matched.add(activity);
            }

            if (matched.size() >= limit) {
                break;
            }
        }

        Collections.reverse(matched);
        return matched;
    }

    public synchronized List<ConnectionActivity> findRecent(int limit) {
        List<ConnectionActivity> matched = new ArrayList<ConnectionActivity>();

        for (int index = activities.size() - 1; index >= 0; index -= 1) {
            matched.add(activities.get(index));

            if (matched.size() >= limit) {
                break;
            }
        }

        Collections.reverse(matched);
        return matched;
    }

    private void persist() {
        File storageFile = new File(storageFilePath);
        File parent = storageFile.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("failed to create activity storage directory");
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storageFile, activities);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to persist connection activities", exception);
        }
    }
}
