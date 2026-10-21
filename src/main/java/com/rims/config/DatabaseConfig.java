package com.rims.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(
            Environment env,
            @Value("${spring.datasource.url:}") String propertyUrl,
            @Value("${spring.datasource.username:}") String propertyUser,
            @Value("${spring.datasource.password:}") String propertyPassword,
            @Value("${spring.datasource.driver-class-name:}") String propertyDriver) {

        // Check potential sources for database connection string
        String rawUrl = env.getProperty("NEON_DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = env.getProperty("DATABASE_URL");
        }
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = propertyUrl;
        }

        if (rawUrl != null) {
            rawUrl = rawUrl.trim();
            // Strip any surrounding single or double quotes
            if ((rawUrl.startsWith("\"") && rawUrl.endsWith("\"")) || 
                (rawUrl.startsWith("'") && rawUrl.endsWith("'"))) {
                rawUrl = rawUrl.substring(1, rawUrl.length() - 1).trim();
            }
        }

        // Check if this is a PostgreSQL / Neon connection
        if (rawUrl != null && isPostgresConnection(rawUrl)) {
            return buildPostgresDataSource(rawUrl, propertyUser, propertyPassword);
        }

        // Fallback: Default / H2 DataSource
        log.info("[RIMS Database] Initializing local H2 DataSource: {}", rawUrl);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(rawUrl != null && !rawUrl.isBlank() ? rawUrl : "jdbc:h2:file:./data/rimsdb;AUTO_SERVER=TRUE");
        config.setDriverClassName(propertyDriver != null && !propertyDriver.isBlank() ? propertyDriver : "org.h2.Driver");
        config.setUsername(propertyUser != null && !propertyUser.isBlank() ? propertyUser : "sa");
        config.setPassword(propertyPassword != null ? propertyPassword : "password");
        config.setPoolName("RimsH2HikariPool");
        return new HikariDataSource(config);
    }

    private boolean isPostgresConnection(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("postgres://") || 
               lower.startsWith("postgresql://") || 
               lower.startsWith("jdbc:postgresql://") ||
               lower.contains(".neon.tech");
    }

    private DataSource buildPostgresDataSource(String rawUrl, String defaultUser, String defaultPass) {
        String jdbcUrl;
        String username = defaultUser;
        String password = defaultPass;

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            jdbcUrl = rawUrl;
            // Ensure sslmode=require for Neon if not specified
            if (rawUrl.contains(".neon.tech") && !rawUrl.contains("sslmode=")) {
                jdbcUrl += (rawUrl.contains("?") ? "&" : "?") + "sslmode=require";
            }
        } else {
            // Convert postgresql:// or postgres:// URI to JDBC URL
            try {
                // Use dummy http scheme to ensure java.net.URI parses authority and userinfo reliably
                String schemeReplaced = rawUrl.replaceFirst("^(?i)postgres(ql)?://", "http://");
                URI uri = URI.create(schemeReplaced);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String dbName = (path != null && path.length() > 1) ? path.substring(1) : "neondb";

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    String[] parts = userInfo.split(":", 2);
                    username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    if (parts.length > 1) {
                        password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                    }
                }

                String query = uri.getRawQuery();
                if (query == null || query.isBlank()) {
                    query = "sslmode=require";
                } else if (!query.contains("sslmode=")) {
                    query = query + "&sslmode=require";
                }

                jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?" + query;
            } catch (Exception e) {
                log.warn("[RIMS Database] Failed to parse URI format, attempting direct jdbc prefix: {}", e.getMessage());
                jdbcUrl = "jdbc:" + rawUrl;
            }
        }

        log.info("[RIMS Database] Connecting to Neon / PostgreSQL: {}", maskJdbcUrl(jdbcUrl));
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.postgresql.Driver");
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        // Production-tuned Neon serverless Hikari settings
        config.setPoolName("RimsNeonHikariPool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000); // 60 seconds
        config.setMaxLifetime(600000); // 10 minutes (avoids stale serverless connections)
        config.setConnectionTimeout(30000); // 30 seconds
        config.setKeepaliveTime(30000); // 30 seconds
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    private String maskJdbcUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }
}
