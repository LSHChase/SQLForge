package com.sqlforge.backend.model;

public class EngineDescriptor {

    private final String code;
    private final String name;
    private final String category;
    private final boolean armReady;

    public EngineDescriptor(String code, String name, String category, boolean armReady) {
        this.code = code;
        this.name = name;
        this.category = category;
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

    public boolean isArmReady() {
        return armReady;
    }
}
