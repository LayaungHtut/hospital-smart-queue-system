package com.hospitalqueue.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configures high-performance HikariCP connection pool for Neon PostgreSQL
 * database.
 * Pre-maintains warm connections to eliminate SSL negotiation latency per
 * query.
 */
@Configuration
public class DatabaseConfig {

    private static final Pattern USER_INFO_PATTERN = Pattern.compile("^jdbc:postgresql://([^/@:]+):([^@]*)@(.+)$");

    @Bean
    public DataSource dataSource(EnvConfig env) {
        String url = env.getOrDefault("NEON_DATABASE_URL",
                "jdbc:postgresql://localhost:5432/hospital_queue");
        String username = env.getOrDefault("NEON_USER", "postgres");
        String password = env.getOrDefault("NEON_PASSWORD", "");

        // Neon URLs come as "postgresql://..." but the JDBC driver requires
        // "jdbc:postgresql://...".
        if (url != null && url.startsWith("postgresql://")) {
            url = "jdbc:" + url;
        }

        // The PostgreSQL JDBC driver cannot parse "user:password@" inside the URL,
        // so move those credentials into setUsername()/setPassword().
        if (url != null) {
            Matcher matcher = USER_INFO_PATTERN.matcher(url);
            if (matcher.matches()) {
                username = matcher.group(1);
                password = matcher.group(2);
                url = "jdbc:postgresql://" + matcher.group(3);
            }
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // Connection pool settings tuned for Neon Cloud + PgBouncer pooler
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(600000); // 10 minutes
        config.setConnectionTimeout(20000); // 20s
        config.setKeepaliveTime(45000); // 45s keepalive
        config.setPoolName("HospitalSmartQueueHikariPool");

        // PgBouncer pooler compatible PostgreSQL JDBC driver optimizations
        config.addDataSourceProperty("prepareThreshold", "0");
        config.addDataSourceProperty("tcpKeepAlive", "true");
        config.addDataSourceProperty("reWriteBatchedInserts", "true");

        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(@NonNull DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
