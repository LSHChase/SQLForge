package com.company.sqlforge.common.jdbc;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.util.StringUtils;

public class ManagedJdbcConnectionFactory {

    private final ConcurrentMap<String, LoadedDriver> loadedDrivers = new ConcurrentHashMap<String, LoadedDriver>();

    public Connection openConnection(String jdbcUrl,
                                     Properties connectionProperties,
                                     String driverClassName,
                                     String driverSourceType,
                                     Path driverJarPath,
                                     String artifactId,
                                     String driverSha256) throws SQLException, ClassNotFoundException {
        if (!StringUtils.hasText(jdbcUrl)) {
            throw new SQLException("jdbcUrl 不能为空");
        }
        if ("UPLOADED".equalsIgnoreCase(trimToNull(driverSourceType))) {
            Driver driver = loadUploadedDriver(driverClassName, driverJarPath, artifactId, driverSha256);
            Connection connection = driver.connect(jdbcUrl, connectionProperties == null ? new Properties() : connectionProperties);
            if (connection == null) {
                throw new SQLException("上传驱动不支持该 jdbcUrl");
            }
            return connection;
        }
        if (StringUtils.hasText(driverClassName)) {
            Class.forName(driverClassName.trim());
        }
        return java.sql.DriverManager.getConnection(jdbcUrl, connectionProperties == null ? new Properties() : connectionProperties);
    }

    private Driver loadUploadedDriver(String driverClassName,
                                      Path driverJarPath,
                                      String artifactId,
                                      String driverSha256) throws SQLException, ClassNotFoundException {
        String normalizedDriverClassName = trimToNull(driverClassName);
        if (!StringUtils.hasText(normalizedDriverClassName)) {
            throw new ClassNotFoundException("UPLOADED 驱动必须提供 driverClassName");
        }
        if (driverJarPath == null || !Files.exists(driverJarPath)) {
            throw new SQLException("上传驱动文件不存在");
        }
        String cacheKey = (trimToNull(artifactId) == null ? normalizedDriverClassName : trimToNull(artifactId))
            + "#" + firstNonBlank(trimToNull(driverSha256), String.valueOf(lastModified(driverJarPath)));
        LoadedDriver loadedDriver = loadedDrivers.get(cacheKey);
        if (loadedDriver == null) {
            loadedDriver = loadedDrivers.computeIfAbsent(cacheKey, ignored -> createLoadedDriver(normalizedDriverClassName, driverJarPath));
        }
        if (loadedDriver.failure != null) {
            if (loadedDriver.failure instanceof RuntimeException) {
                throw (RuntimeException) loadedDriver.failure;
            }
            if (loadedDriver.failure instanceof Error) {
                throw (Error) loadedDriver.failure;
            }
        }
        if (loadedDriver.driver == null) {
            throw new ClassNotFoundException("上传驱动加载失败：" + normalizedDriverClassName);
        }
        return loadedDriver.driver;
    }

    private LoadedDriver createLoadedDriver(String driverClassName, Path driverJarPath) {
        try {
            URL[] urls = new URL[] { toUrl(driverJarPath) };
            URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
            Driver driver = (Driver) Class.forName(driverClassName, true, classLoader).newInstance();
            return new LoadedDriver(driver, classLoader, null);
        } catch (Exception ex) {
            return new LoadedDriver(null, null, ex);
        }
    }

    private URL toUrl(Path driverJarPath) throws MalformedURLException {
        return driverJarPath.toUri().toURL();
    }

    private long lastModified(Path driverJarPath) {
        try {
            return Files.getLastModifiedTime(driverJarPath).toMillis();
        } catch (IOException ex) {
            return -1L;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String left, String right) {
        return StringUtils.hasText(left) ? left : right;
    }

    private static final class LoadedDriver implements Closeable {

        private final Driver driver;
        private final URLClassLoader classLoader;
        private final Throwable failure;

        private LoadedDriver(Driver driver, URLClassLoader classLoader, Throwable failure) {
            this.driver = driver;
            this.classLoader = classLoader;
            this.failure = failure;
        }

        @Override
        public void close() throws IOException {
            if (classLoader != null) {
                classLoader.close();
            }
        }
    }
}
