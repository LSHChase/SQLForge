package com.company.queryexecution.application.service;

import com.company.queryexecution.config.QueryExecutionJdbcAgentRedisProperties;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.util.StringUtils;

class RedisRewriteRuleClient {

    private final QueryExecutionJdbcAgentRedisProperties properties;

    RedisRewriteRuleClient(QueryExecutionJdbcAgentRedisProperties properties) {
        this.properties = properties;
    }

    void set(String key, String value, long ttlSeconds) {
        try {
            Object result = ttlSeconds > 0L
                ? execute("SETEX", key, String.valueOf(ttlSeconds), value)
                : execute("SET", key, value);
            if (!"OK".equalsIgnoreCase(String.valueOf(result))) {
                throw new IllegalStateException("Unexpected Redis SET response");
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    void delete(String key) {
        try {
            execute("DEL", key);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private Object execute(String... args) throws IOException {
        if (properties == null || !StringUtils.hasText(properties.getHost())) {
            throw new IOException("Redis host is not configured");
        }
        Socket socket = new Socket();
        socket.connect(
            new InetSocketAddress(properties.getHost().trim(), properties.getPort()),
            properties.getConnectTimeoutMs()
        );
        socket.setSoTimeout(properties.getReadTimeoutMs());
        try {
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            authenticateIfNeeded(output, input);
            selectDatabaseIfNeeded(output, input);
            writeCommand(output, args);
            output.flush();
            return readResp(input);
        } finally {
            socket.close();
        }
    }

    private void authenticateIfNeeded(BufferedOutputStream output, BufferedInputStream input) throws IOException {
        if (!StringUtils.hasText(properties.getPassword())) {
            return;
        }
        writeCommand(output, "AUTH", properties.getPassword().trim());
        output.flush();
        readResp(input);
    }

    private void selectDatabaseIfNeeded(BufferedOutputStream output, BufferedInputStream input) throws IOException {
        if (properties.getDatabase() <= 0) {
            return;
        }
        writeCommand(output, "SELECT", String.valueOf(properties.getDatabase()));
        output.flush();
        readResp(input);
    }

    private void writeCommand(BufferedOutputStream output, String... args) throws IOException {
        output.write(("*" + args.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        for (String arg : args) {
            byte[] bytes = (arg == null ? "" : arg).getBytes(StandardCharsets.UTF_8);
            output.write(("$" + bytes.length + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(bytes);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private Object readResp(BufferedInputStream input) throws IOException {
        int type = input.read();
        if (type == -1) {
            throw new IOException("Redis closed connection");
        }
        if (type == '+') {
            return readLine(input);
        }
        if (type == '-') {
            throw new IOException(readLine(input));
        }
        if (type == ':') {
            return Long.valueOf(readLine(input));
        }
        if (type == '$') {
            int length = Integer.parseInt(readLine(input));
            if (length < 0) {
                return null;
            }
            byte[] bytes = readBytes(input, length);
            readBytes(input, 2);
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (type == '*') {
            int count = Integer.parseInt(readLine(input));
            if (count < 0) {
                return Collections.emptyList();
            }
            List<Object> values = new ArrayList<Object>();
            for (int index = 0; index < count; index++) {
                values.add(readResp(input));
            }
            return values;
        }
        throw new IOException("Unsupported Redis response type");
    }

    private String readLine(BufferedInputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int previous = -1;
        int current;
        while ((current = input.read()) != -1) {
            if (previous == '\r' && current == '\n') {
                byte[] raw = buffer.toByteArray();
                return new String(raw, 0, raw.length - 1, StandardCharsets.UTF_8);
            }
            buffer.write(current);
            previous = current;
        }
        throw new IOException("Redis response line is incomplete");
    }

    private byte[] readBytes(BufferedInputStream input, int length) throws IOException {
        byte[] bytes = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(bytes, offset, length - offset);
            if (read == -1) {
                throw new IOException("Redis bulk response is incomplete");
            }
            offset += read;
        }
        return bytes;
    }
}
