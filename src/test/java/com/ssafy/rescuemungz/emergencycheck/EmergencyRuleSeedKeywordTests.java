package com.ssafy.rescuemungz.emergencycheck;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyRuleSeedKeywordTests {

    @Test
    void limpingRulesIncludeNaturalGuardianExpressions() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/data.sql"), StandardCharsets.UTF_8);

        assertRuleContains(sql, 5,
                "\uC090\uB057", "\uB2E4\uB9AC\uB97C \uC808", "\uC808\uC5B4\uC694", "\uCA54\uB69D");
        assertRuleContains(sql, 104,
                "\uC090\uB057", "\uB2E4\uB9AC\uB97C \uC808", "\uC808\uC5B4\uC694", "\uCA54\uB69D");
        assertRuleContains(sql, 108,
                "\uC090\uB057", "\uC090\uB057\uD588", "\uB2E4\uB9AC\uB97C \uC808", "\uC808\uC5B4\uC694", "\uCA54\uB69D");
        assertRuleContains(sql, 9,
                "\uB208 \uBE44\uBE44", "\uB208\uC744 \uBE44\uBCBC", "\uB208 \uBE68\uAC1B", "\uBE68\uAC8C");
    }

    private static void assertRuleContains(String sql, int ruleId, String... keywords) {
        String ruleSql = ruleSql(sql, ruleId);
        for (String keyword : keywords) {
            assertThat(ruleSql).contains(keyword);
        }
    }

    private static String ruleSql(String sql, int ruleId) {
        int rulesInsert = sql.indexOf("INSERT INTO `emergency_rules`");
        assertThat(rulesInsert).isNotNegative();
        String emergencyRulesSql = sql.substring(rulesInsert);

        int start = emergencyRulesSql.indexOf("(" + ruleId + ", ");
        assertThat(start).isNotNegative();

        int commaEnd = emergencyRulesSql.indexOf("),", start);
        int semicolonEnd = emergencyRulesSql.indexOf(");", start);
        int end = commaEnd >= 0 ? commaEnd : semicolonEnd;
        assertThat(end).isGreaterThan(start);
        return emergencyRulesSql.substring(start, end);
    }
}
