package com.sqlforge.backend.model;

public class EngineDescriptor {

    private final String code;
    private final String name;
    private final String category;
    private final int defaultPort;
    private final String transport;
    private final String jdbcScheme;
    private final String driverClassName;
    private final String profileNote;
    private final boolean armReady;

    public EngineDescriptor(
        String code,
        String name,
        String category,
        int defaultPort,
        String transport,
        String jdbcScheme,
        String driverClassName,
        String profileNote,
        boolean armReady
    ) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.defaultPort = defaultPort;
        this.transport = transport;
        this.jdbcScheme = jdbcScheme;
        this.driverClassName = driverClassName;
        this.profileNote = profileNote;
        this.armReady = armReady;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getTransport() {
        return transport;
    }

    public String getJdbcScheme() {
        return jdbcScheme;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getProfileNote() {
        return profileNote;
    }

    public boolean isArmReady() {
        return armReady;
    }
}
