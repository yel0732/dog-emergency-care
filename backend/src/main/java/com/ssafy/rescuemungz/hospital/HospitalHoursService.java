package com.ssafy.rescuemungz.hospital;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HospitalHoursService {
    private final HospitalMapper mapper;
    private final GooglePlacesClient googlePlacesClient;
    private final ObjectMapper objectMapper;

    public HospitalHoursService(HospitalMapper mapper, GooglePlacesClient googlePlacesClient, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.googlePlacesClient = googlePlacesClient;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return googlePlacesClient.isConfigured();
    }

    public HospitalHoursResult syncOpeningHours(int limit) {
        return syncOpeningHours(limit, null, null, null, null, null, null);
    }

    public HospitalHoursResult syncOpeningHours(int limit, String keyword, BigDecimal lat, BigDecimal lng,
                                                String status, String sido, String sigungu) {
        if (!googlePlacesClient.isConfigured()) {
            return new HospitalHoursResult(0, 0, 0, 0, 0,
                    "Google Places API 키(GOOGLE_PLACES_API_KEY)가 설정되지 않았습니다.");
        }

        int safeLimit = Math.max(1, Math.min(limit, 60));
        List<Hospital> targets = mapper.findNeedsHours(keyword, lat, lng, status, sido, sigungu, safeLimit);
        int updated = 0;
        int open24 = 0;
        int night = 0;
        int failed = 0;

        for (Hospital hospital : targets) {
            GooglePlacesClient.PlaceHours hours;
            try {
                hours = googlePlacesClient.fetchHours(hospital.getName(), hospital.getAddress());
            } catch (GooglePlacesException e) {
                markAttempted(targets);
                return new HospitalHoursResult(targets.size(), updated, open24, night,
                        targets.size() - updated, e.getMessage());
            }

            if (hours == null || (hours.openingHours() == null && hours.phone() == null)) {
                mapper.markHoursAttempted(hospital.getId());
                failed++;
                continue;
            }

            int affected;
            try {
                affected = mapper.updateOpeningHours(
                        hospital.getId(),
                        jsonString(hours.openingHours() == null ? "영업시간 확인 필요" : hours.openingHours()),
                        hours.phone(),
                        hours.open24(),
                        hours.nightAvailable(),
                        hours.weekendAvailable()
                );
            } catch (DataAccessException e) {
                mapper.markHoursAttempted(hospital.getId());
                failed++;
                continue;
            }
            if (affected > 0) {
                updated++;
                if (hours.open24()) {
                    open24++;
                }
                if (hours.nightAvailable()) {
                    night++;
                }
            } else {
                failed++;
            }
        }

        String message = updated == 0
                ? "영업시간을 가져온 병원이 없습니다. 병원명, 주소 또는 Google Places 설정을 확인해 주세요."
                : "영업시간 동기화를 완료했습니다.";
        return new HospitalHoursResult(targets.size(), updated, open24, night, failed, message);
    }

    private void markAttempted(List<Hospital> targets) {
        for (Hospital target : targets) {
            mapper.markHoursAttempted(target.getId());
        }
    }

    private String jsonString(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
