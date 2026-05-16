package com.company.sqlforge.common.config;

/**
 * 可信入口传递的受支持认证来源取值。
 */
public final class AuthSourceConstants {

    public static final String GATEWAY = "gateway";
    public static final String HEADER = "header";
    public static final String TOKEN = "token";

    private AuthSourceConstants() {
    }
}
