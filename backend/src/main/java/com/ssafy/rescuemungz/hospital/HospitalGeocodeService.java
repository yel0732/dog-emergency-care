package com.ssafy.rescuemungz.hospital;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HospitalGeocodeService {
    private final HospitalMapper mapper;
    private final KakaoLocalClient kakaoLocalClient;

    public HospitalGeocodeService(HospitalMapper mapper, KakaoLocalClient kakaoLocalClient) {
        this.mapper = mapper;
        this.kakaoLocalClient = kakaoLocalClient;
    }

    public boolean isConfigured() {
        return kakaoLocalClient.isConfigured();
    }

    public HospitalGeocodeResult geocodeMissing(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return geocodeTargets(mapper.findNeedsGeocoding(safeLimit));
    }

    public HospitalGeocodeResult geocodeTargets(List<Hospital> targets) {
        int updated = 0;
        int failed = 0;
        String message = "좌표 보정을 완료했습니다.";

        for (Hospital hospital : targets) {
            KakaoLocalClient.KakaoCoordinate coordinate;
            KakaoLocalClient.KakaoPlace place;
            try {
                place = kakaoLocalClient.searchPlace(hospital.getName(), hospital.getAddress());
                coordinate = place == null ? kakaoLocalClient.searchAddress(hospital.getAddress()) : null;
            } catch (KakaoLocalException e) {
                return new HospitalGeocodeResult(targets.size(), updated, targets.size() - updated, e.getMessage());
            }
            if (place == null && coordinate == null) {
                mapper.markGeocodeAttempted(hospital.getId());
                failed++;
                continue;
            }

            BigDecimal lat = place == null ? coordinate.lat() : place.lat();
            BigDecimal lng = place == null ? coordinate.lng() : place.lng();
            String placeId = place == null ? coordinate.placeId() : place.placeId();
            String phone = place == null ? null : place.phone();

            int affected = mapper.updateKakaoDetails(hospital.getId(), lat, lng, placeId, phone);
            if (affected > 0) {
                updated++;
            } else {
                failed++;
            }
        }

        if (updated == 0 && failed > 0) {
            message = "주소 검색에 실패한 병원이 있습니다. Kakao 설정과 병원 주소를 확인해 주세요.";
        }
        return new HospitalGeocodeResult(targets.size(), updated, failed, message);
    }
}
