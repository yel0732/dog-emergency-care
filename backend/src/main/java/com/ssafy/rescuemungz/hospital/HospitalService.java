package com.ssafy.rescuemungz.hospital;

import com.ssafy.rescuemungz.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HospitalService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("open", "normal", "emergency24", "night", "weekend");
    private static final int AUTO_GEOCODE_LIMIT = 5;
    private static final int AUTO_HOURS_SYNC_LIMIT = 60;

    private final HospitalMapper mapper;
    private final HospitalGeocodeService geocodeService;
    private final HospitalHoursService hoursService;

    public HospitalService(HospitalMapper mapper, HospitalGeocodeService geocodeService, HospitalHoursService hoursService) {
        this.mapper = mapper;
        this.geocodeService = geocodeService;
        this.hoursService = hoursService;
    }

    public List<Hospital> search(String keyword, BigDecimal lat, BigDecimal lng, boolean emergencyOnly, boolean nightOnly,
                                 boolean phoneOnly, boolean locatedOnly, String status, String sido, String sigungu) {
        String safeKeyword = blankToNull(keyword);
        String safeStatus = safeStatus(status);
        String safeSido = blankToNull(sido);
        String safeSigungu = blankToNull(sigungu);
        List<Hospital> hospitals = mapper.search(safeKeyword, lat, lng, emergencyOnly, nightOnly, phoneOnly, locatedOnly,
                safeStatus, safeSido, safeSigungu);
        if (!locatedOnly && geocodeService.isConfigured() && needsCoordinates(hospitals)) {
            geocodeService.geocodeTargets(geocodeTargets(hospitals));
            hospitals = mapper.search(safeKeyword, lat, lng, emergencyOnly, nightOnly, phoneOnly, locatedOnly,
                    safeStatus, safeSido, safeSigungu);
        }
        if (hoursService.isConfigured() && needsHours(hospitals)) {
            hoursService.syncOpeningHours(AUTO_HOURS_SYNC_LIMIT, safeKeyword, lat, lng, safeStatus, safeSido, safeSigungu);
            hospitals = mapper.search(safeKeyword, lat, lng, emergencyOnly, nightOnly, phoneOnly, locatedOnly,
                    safeStatus, safeSido, safeSigungu);
        }
        return hospitals;
    }

    public HospitalRegionResponse regions() {
        List<HospitalRegionCount> counts = mapper.findSidoCounts();
        Set<String> mergedSidos = new LinkedHashSet<>(mapper.findSidos());
        counts.stream()
                .map(HospitalRegionCount::getSido)
                .filter(sido -> sido != null && !sido.isBlank())
                .forEach(mergedSidos::add);
        List<String> sidos = new ArrayList<>(mergedSidos);
        return new HospitalRegionResponse(sidos, sidos.stream()
                .collect(Collectors.toMap(sido -> sido, mapper::findSigungus)), counts);
    }

    public Hospital find(long id) {
        Hospital hospital = mapper.findById(id);
        if (hospital == null) {
            throw new NotFoundException("Hospital not found.");
        }
        return hospital;
    }

    public HospitalGeocodeResult geocodeMissing(int limit) {
        return geocodeService.geocodeMissing(limit);
    }

    public HospitalHoursResult syncOpeningHours(int limit) {
        return hoursService.syncOpeningHours(limit);
    }

    public HospitalHoursResult syncOpeningHours(int limit, String keyword, BigDecimal lat, BigDecimal lng,
                                                String status, String sido, String sigungu) {
        return hoursService.syncOpeningHours(limit, blankToNull(keyword), lat, lng, safeStatus(status),
                blankToNull(sido), blankToNull(sigungu));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String safeStatus(String value) {
        String status = blankToNull(value);
        if (status == null) {
            return null;
        }
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("병원 상태 필터는 open, normal, emergency24, night, weekend 중 하나여야 합니다.");
        }
        return status;
    }

    private boolean needsHours(List<Hospital> hospitals) {
        return hospitals.stream()
                .limit(AUTO_HOURS_SYNC_LIMIT)
                .anyMatch(this::needsOpeningHours);
    }

    private boolean needsOpeningHours(Hospital hospital) {
        String openingHours = hospital.getOpeningHours();
        if (openingHours == null || openingHours.isBlank()) {
            return true;
        }
        String normalized = openingHours.trim().replaceAll("^\"|\"$", "");
        return "영업시간 확인 필요".equals(normalized);
    }

    private boolean needsCoordinates(List<Hospital> hospitals) {
        return hospitals.stream()
                .limit(AUTO_GEOCODE_LIMIT)
                .anyMatch(this::needsCoordinate);
    }

    private List<Hospital> geocodeTargets(List<Hospital> hospitals) {
        LocalDateTime retryBefore = LocalDateTime.now().minusDays(1);
        return hospitals.stream()
                .filter(this::needsCoordinate)
                .filter(hospital -> hospital.getLastVerifiedAt() == null
                        || hospital.getLastVerifiedAt().isBefore(retryBefore))
                .limit(AUTO_GEOCODE_LIMIT)
                .toList();
    }

    private boolean needsCoordinate(Hospital hospital) {
        return hospital.getLat() == null
                || hospital.getLng() == null;
    }
}
