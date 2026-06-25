package com.ssafy.rescuemungz.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserSchemaCompatibility implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(UserSchemaCompatibility.class);

    private final JdbcTemplate jdbcTemplate;

    public UserSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn("login_id", "ALTER TABLE users ADD COLUMN login_id VARCHAR(50) NULL AFTER id");
        ensureColumn("name", "ALTER TABLE users ADD COLUMN name VARCHAR(50) NULL AFTER password_hash");
        ensureIndex("uk_users_login_id", "ALTER TABLE users ADD UNIQUE KEY uk_users_login_id (login_id)");
        backfillIdentityColumns();
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY profile_image LONGTEXT NULL");
        } catch (Exception e) {
            log.warn("Skipped profile_image column compatibility adjustment.", e);
        }
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND column_name = ?
                """, Integer.class, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void ensureIndex(String indexName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'users'
                  AND index_name = ?
                """, Integer.class, indexName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void backfillIdentityColumns() {
        jdbcTemplate.update("""
                UPDATE users
                SET login_id = nickname
                WHERE (login_id IS NULL OR login_id = '')
                  AND nickname IS NOT NULL
                  AND nickname <> ''
                """);
        jdbcTemplate.update("""
                UPDATE users
                SET name = nickname
                WHERE (name IS NULL OR name = '')
                  AND nickname IS NOT NULL
                  AND nickname <> ''
                """);
    }
}
