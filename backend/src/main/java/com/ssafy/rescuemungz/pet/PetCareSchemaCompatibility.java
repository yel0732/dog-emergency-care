package com.ssafy.rescuemungz.pet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PetCareSchemaCompatibility implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    public PetCareSchemaCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureColumn(
                "emergency_check_id",
                "ALTER TABLE care_diaries ADD COLUMN emergency_check_id BIGINT NULL AFTER pet_id"
        );
        ensureIndex(
                "idx_care_emergency_check_id",
                "CREATE INDEX idx_care_emergency_check_id ON care_diaries (emergency_check_id)"
        );
        jdbcTemplate.execute("""
                ALTER TABLE care_diaries
                MODIFY category ENUM('응급 체크','병원 진료','진료','예방접종','접종','약 복용','검진','케어 루틴') NOT NULL
                """);
    }

    private void ensureColumn(String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'care_diaries'
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
                  AND table_name = 'care_diaries'
                  AND index_name = ?
                """, Integer.class, indexName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
