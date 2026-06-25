package com.ssafy.rescuemungz.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.caseboard.CaseBoardService;
import com.ssafy.rescuemungz.emergencyvideo.EmergencyVideoService;
import com.ssafy.rescuemungz.hospital.HospitalService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryValidationTests {
    @Test
    void emergencyVideoRejectsUnknownSort() {
        EmergencyVideoService service = new EmergencyVideoService(null);

        assertThatThrownBy(() -> service.search(null, null, "drop", "desc", 0, 6, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("latest")
                .hasMessageContaining("bookmarks");
    }

    @Test
    void caseBoardRejectsUnknownDirection() {
        CaseBoardService service = new CaseBoardService(null, null, new ObjectMapper());

        assertThatThrownBy(() -> service.search(null, null, null, "latest", "sideways", false, 0, 6, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asc")
                .hasMessageContaining("desc");
    }

    @Test
    void hospitalRejectsUnknownStatus() {
        HospitalService service = new HospitalService(null, null, null);

        assertThatThrownBy(() -> service.search(null, null, null, false, false, false, false,
                "always", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("open")
                .hasMessageContaining("weekend");
    }

    @Test
    void frontendQueryOptionsStayInSyncWithBackendContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode frontendContract = mapper.readTree(Files.readString(Path.of("frontend/src/constants/query-options.contract.json")));

        assertThat(frontendContract.path("defaults").path("sort").asText()).isEqualTo(QueryOptionContract.DEFAULT_SORT);
        assertThat(frontendContract.path("defaults").path("direction").asText()).isEqualTo(QueryOptionContract.DEFAULT_DIRECTION);
        assertThat(values(frontendContract, "videoSorts")).containsExactlyElementsOf(QueryOptionContract.VIDEO_SORTS);
        assertThat(values(frontendContract, "caseSorts")).containsExactlyElementsOf(QueryOptionContract.CASE_SORTS);
        assertThat(values(frontendContract, "directions")).containsExactlyElementsOf(QueryOptionContract.DIRECTIONS);
    }

    private static List<String> values(JsonNode root, String fieldName) {
        List<String> values = new ArrayList<>();
        root.path(fieldName).forEach(node -> values.add(node.path("value").asText()));
        return values;
    }
}
