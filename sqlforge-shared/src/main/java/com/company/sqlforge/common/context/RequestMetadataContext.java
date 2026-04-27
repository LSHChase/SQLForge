package com.company.sqlforge.common.context;

public final class RequestMetadataContext {

    private static final ThreadLocal<RequestMetadata> HOLDER = new ThreadLocal<RequestMetadata>();

    private RequestMetadataContext() {
    }

    public static void set(String sourceIp, String userAgent) {
        set(sourceIp, userAgent, null);
    }

    public static void set(String sourceIp, String userAgent, String accessChannel) {
        HOLDER.set(new RequestMetadata(sourceIp, userAgent, accessChannel));
    }

    public static String getSourceIp() {
        RequestMetadata metadata = HOLDER.get();
        return metadata == null ? null : metadata.getSourceIp();
    }

    public static String getUserAgent() {
        RequestMetadata metadata = HOLDER.get();
        return metadata == null ? null : metadata.getUserAgent();
    }

    public static String getAccessChannel() {
        RequestMetadata metadata = HOLDER.get();
        return metadata == null ? null : metadata.getAccessChannel();
    }

    public static void clear() {
        HOLDER.remove();
    }

    private static final class RequestMetadata {

        private final String sourceIp;
        private final String userAgent;
        private final String accessChannel;

        private RequestMetadata(String sourceIp, String userAgent, String accessChannel) {
            this.sourceIp = sourceIp;
            this.userAgent = userAgent;
            this.accessChannel = accessChannel;
        }

        private String getSourceIp() {
            return sourceIp;
        }

        private String getUserAgent() {
            return userAgent;
        }

        private String getAccessChannel() {
            return accessChannel;
        }
    }
}
