package com.ssafy.rescuemungz.hospital;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface HospitalMapper {
    @Select("""
            <script>
            SELECT id, hospital_name AS name, address, phone, lat, lng,
                   CASE
                       WHEN COALESCE(is_24h, FALSE) = TRUE
                         OR hospital_name LIKE '%24시%'
                         OR hospital_name LIKE '%24시간%'
                         OR opening_hours LIKE '%24시%'
                         OR opening_hours LIKE '%24시간%'
                       THEN TRUE ELSE FALSE
                   END AS is_24h,
                   opening_hours, emergency_available,
                   night_available, holiday_available, operating_status, kakao_place_id, last_verified_at, updated_at,
                   CASE
                       WHEN #{lat} IS NULL OR #{lng} IS NULL OR lat IS NULL OR lng IS NULL THEN NULL
                       ELSE (6371 * ACOS(LEAST(1, GREATEST(-1,
                           COS(RADIANS(#{lat})) * COS(RADIANS(lat)) *
                           COS(RADIANS(lng) - RADIANS(#{lng})) +
                           SIN(RADIANS(#{lat})) * SIN(RADIANS(lat))
                       ))))
                   END AS distance_km
            FROM hospitals
            <where>
                AND hospital_name &lt;&gt; '백산동물병원'
                <if test="keyword != null">
                    AND (hospital_name LIKE CONCAT('%', #{keyword}, '%')
                         OR address LIKE CONCAT('%', #{keyword}, '%')
                         OR phone LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="sido != null">
                    AND address LIKE CONCAT(#{sido}, '%')
                </if>
                <if test="sigungu != null">
                    AND (address LIKE CONCAT('% ', #{sigungu}, ' %') OR address LIKE CONCAT('% ', #{sigungu}))
                </if>
                <if test="emergencyOnly">
                    AND (
                        emergency_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="nightOnly">
                    AND (
                        night_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="phoneOnly">
                    AND phone IS NOT NULL AND phone &lt;&gt; ''
                </if>
                <if test="locatedOnly">
                    AND lat IS NOT NULL AND lng IS NOT NULL
                </if>
                <if test="status == 'emergency24'">
                    AND (
                        is_24h = TRUE
                        OR emergency_available = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="status == 'normal'">
                    AND COALESCE(is_24h, FALSE) = FALSE
                    AND COALESCE(emergency_available, FALSE) = FALSE
                    AND COALESCE(night_available, FALSE) = FALSE
                    AND COALESCE(holiday_available, FALSE) = FALSE
                    AND hospital_name NOT LIKE '%24시%'
                    AND hospital_name NOT LIKE '%24시간%'
                    AND (opening_hours IS NULL OR (opening_hours NOT LIKE '%24시%' AND opening_hours NOT LIKE '%24시간%'))
                </if>
                <if test="status == 'night'">
                    AND (
                        night_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="status == 'weekend'">
                    AND (
                        holiday_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
            </where>
            ORDER BY distance_km IS NULL, distance_km, is_24h DESC, emergency_available DESC, id
            </script>
            """)
    List<Hospital> search(@Param("keyword") String keyword,
                          @Param("lat") BigDecimal lat,
                          @Param("lng") BigDecimal lng,
                          @Param("emergencyOnly") boolean emergencyOnly,
                          @Param("nightOnly") boolean nightOnly,
                          @Param("phoneOnly") boolean phoneOnly,
                          @Param("locatedOnly") boolean locatedOnly,
                          @Param("status") String status,
                          @Param("sido") String sido,
                          @Param("sigungu") String sigungu);

    @Select("""
            SELECT DISTINCT SUBSTRING_INDEX(address, ' ', 1)
            FROM hospitals
            WHERE hospital_name <> '백산동물병원'
              AND address IS NOT NULL AND address <> ''
            ORDER BY 1
            """)
    List<String> findSidos();

    @Select("""
            SELECT SUBSTRING_INDEX(address, ' ', 1) AS sido, COUNT(*) AS count
            FROM hospitals
            WHERE hospital_name <> '백산동물병원'
              AND address IS NOT NULL AND address <> ''
            GROUP BY SUBSTRING_INDEX(address, ' ', 1)
            ORDER BY count DESC, sido
            """)
    List<HospitalRegionCount> findSidoCounts();

    @Select("""
            SELECT DISTINCT SUBSTRING_INDEX(SUBSTRING_INDEX(address, ' ', 2), ' ', -1)
            FROM hospitals
            WHERE hospital_name <> '백산동물병원'
              AND address LIKE CONCAT(#{sido}, ' %')
            ORDER BY 1
            """)
    List<String> findSigungus(String sido);

    @Select("""
            SELECT id, hospital_name AS name, address, phone, lat, lng,
                   CASE
                       WHEN COALESCE(is_24h, FALSE) = TRUE
                         OR hospital_name LIKE '%24시%'
                         OR hospital_name LIKE '%24시간%'
                         OR opening_hours LIKE '%24시%'
                         OR opening_hours LIKE '%24시간%'
                       THEN TRUE ELSE FALSE
                   END AS is_24h,
                   opening_hours, emergency_available,
                   night_available, holiday_available, operating_status, kakao_place_id, last_verified_at, updated_at
            FROM hospitals
            WHERE id = #{id}
              AND hospital_name <> '백산동물병원'
            """)
    Hospital findById(long id);

    @Select("""
            SELECT id, hospital_name AS name, address, phone, lat, lng,
                   CASE
                       WHEN COALESCE(is_24h, FALSE) = TRUE
                         OR hospital_name LIKE '%24시%'
                         OR hospital_name LIKE '%24시간%'
                         OR opening_hours LIKE '%24시%'
                         OR opening_hours LIKE '%24시간%'
                       THEN TRUE ELSE FALSE
                   END AS is_24h,
                   opening_hours, emergency_available,
                   night_available, holiday_available, operating_status, kakao_place_id, last_verified_at, updated_at
            FROM hospitals
            WHERE (lat IS NULL OR lng IS NULL)
              AND hospital_name <> '백산동물병원'
              AND address IS NOT NULL
              AND address <> ''
              AND (last_verified_at IS NULL OR last_verified_at < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY))
            ORDER BY id
            LIMIT #{limit}
            """)
    List<Hospital> findNeedsGeocoding(@Param("limit") int limit);

    @Update("""
            UPDATE hospitals
            SET last_verified_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int markGeocodeAttempted(@Param("id") long id);

    @Update("""
            UPDATE hospitals
            SET lat = COALESCE(#{lat}, lat),
                lng = COALESCE(#{lng}, lng),
                kakao_place_id = COALESCE(#{kakaoPlaceId}, kakao_place_id),
                phone = CASE
                    WHEN (phone IS NULL OR phone = '') AND #{phone} IS NOT NULL AND #{phone} <> '' THEN #{phone}
                    ELSE phone
                END,
                last_verified_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateKakaoDetails(@Param("id") long id,
                           @Param("lat") BigDecimal lat,
                           @Param("lng") BigDecimal lng,
                           @Param("kakaoPlaceId") String kakaoPlaceId,
                           @Param("phone") String phone);

    @Select("""
            <script>
            SELECT id, hospital_name AS name, address, phone, lat, lng,
                   CASE
                       WHEN COALESCE(is_24h, FALSE) = TRUE
                         OR hospital_name LIKE '%24시%'
                         OR hospital_name LIKE '%24시간%'
                         OR opening_hours LIKE '%24시%'
                         OR opening_hours LIKE '%24시간%'
                       THEN TRUE ELSE FALSE
                   END AS is_24h,
                   opening_hours, emergency_available,
                   night_available, holiday_available, operating_status, kakao_place_id, last_verified_at, updated_at,
                   CASE
                       WHEN #{lat} IS NULL OR #{lng} IS NULL OR lat IS NULL OR lng IS NULL THEN NULL
                       ELSE (6371 * ACOS(LEAST(1, GREATEST(-1,
                           COS(RADIANS(#{lat})) * COS(RADIANS(lat)) *
                           COS(RADIANS(lng) - RADIANS(#{lng})) +
                           SIN(RADIANS(#{lat})) * SIN(RADIANS(lat))
                       ))))
                   END AS distance_km
            FROM hospitals
            <where>
                AND (opening_hours IS NULL OR JSON_UNQUOTE(opening_hours) = '영업시간 확인 필요')
                AND (last_verified_at IS NULL OR last_verified_at &lt; DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY))
                AND hospital_name &lt;&gt; '백산동물병원'
                AND hospital_name IS NOT NULL AND hospital_name &lt;&gt; ''
                <if test="keyword != null">
                    AND (hospital_name LIKE CONCAT('%', #{keyword}, '%')
                         OR address LIKE CONCAT('%', #{keyword}, '%')
                         OR phone LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="sido != null">
                    AND address LIKE CONCAT(#{sido}, '%')
                </if>
                <if test="sigungu != null">
                    AND (address LIKE CONCAT('% ', #{sigungu}, ' %') OR address LIKE CONCAT('% ', #{sigungu}))
                </if>
                <if test="status == 'emergency24'">
                    AND (
                        is_24h = TRUE
                        OR emergency_available = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="status == 'normal'">
                    AND COALESCE(is_24h, FALSE) = FALSE
                    AND COALESCE(emergency_available, FALSE) = FALSE
                    AND COALESCE(night_available, FALSE) = FALSE
                    AND COALESCE(holiday_available, FALSE) = FALSE
                    AND hospital_name NOT LIKE '%24시%'
                    AND hospital_name NOT LIKE '%24시간%'
                    AND (opening_hours IS NULL OR (opening_hours NOT LIKE '%24시%' AND opening_hours NOT LIKE '%24시간%'))
                </if>
                <if test="status == 'night'">
                    AND (
                        night_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
                <if test="status == 'weekend'">
                    AND (
                        holiday_available = TRUE
                        OR is_24h = TRUE
                        OR hospital_name LIKE '%24시%'
                        OR hospital_name LIKE '%24시간%'
                        OR opening_hours LIKE '%24시%'
                        OR opening_hours LIKE '%24시간%'
                    )
                </if>
            </where>
            ORDER BY distance_km IS NULL, distance_km, last_verified_at IS NULL DESC, last_verified_at, id
            LIMIT #{limit}
            </script>
            """)
    List<Hospital> findNeedsHours(@Param("keyword") String keyword,
                                  @Param("lat") BigDecimal lat,
                                  @Param("lng") BigDecimal lng,
                                  @Param("status") String status,
                                  @Param("sido") String sido,
                                  @Param("sigungu") String sigungu,
                                  @Param("limit") int limit);

    @Update("""
            UPDATE hospitals
            SET last_verified_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int markHoursAttempted(@Param("id") long id);

    @Update("""
            UPDATE hospitals
            SET opening_hours = #{openingHours},
                is_24h = #{is24h},
                night_available = CASE WHEN #{nightAvailable} THEN TRUE ELSE night_available END,
                holiday_available = CASE WHEN #{holidayAvailable} THEN TRUE ELSE holiday_available END,
                emergency_available = CASE WHEN #{is24h} THEN TRUE ELSE emergency_available END,
                phone = CASE
                    WHEN (phone IS NULL OR phone = '') AND #{phone} IS NOT NULL AND #{phone} <> '' THEN #{phone}
                    ELSE phone
                END,
                last_verified_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int updateOpeningHours(@Param("id") long id,
                           @Param("openingHours") String openingHours,
                           @Param("phone") String phone,
                           @Param("is24h") boolean is24h,
                           @Param("nightAvailable") boolean nightAvailable,
                           @Param("holidayAvailable") boolean holidayAvailable);
}
