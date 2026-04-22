package com.company.queryexecution.application.context;

public final class RequestMetadataContext {

    private static final ThreadLocal<RequestMetadata> HOLDER = new ThreadLocal<RequestMetadata>();

    private RequestMetadataContext() {
    }

    public static void set(String sourceIp, String userAgent) {
        HOLDER.set(new RequestMetadata(sourceIp, userAgent));
    }

    public static String getSourceIp() {
        RequestMetadata metadata = HOLDER.get();
        return metadata == null ? null : metadata.getSourceIp();
    }

    public static String getUserAgent() {
        RequestMetadata metadata = HOLDER.get();
        return metadata == null ? null : metadata.getUserAgent();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static final class RequestMetadata {

        private final String sourceIp;
        private final String userAgent;

        private RequestMetadata(String sourceIp, String userAgent) {
            this.sourceIp = sourceIp;
            this.userAgent = userAgent;
        }

        private String getSourceIp() {
            return sourceIp;
        }

        private String getUserAgent() {
            return userAgent;
        }
    }
}
