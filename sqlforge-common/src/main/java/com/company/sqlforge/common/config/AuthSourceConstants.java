package com.company.sqlforge.common.config;

/**
 * Supported authentication source values propagated by trusted entrypoints.
 */
public final class AuthSourceConstants {

    public static final String GATEWAY = "gateway";
    public static final String HEADER = "header";
    public static final String TOKEN = "token";

    private AuthSourceConstants() {
    }
}
