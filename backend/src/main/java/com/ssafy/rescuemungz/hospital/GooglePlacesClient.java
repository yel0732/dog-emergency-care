package com.ssafy.rescuemungz.hospital;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 구글 Places API (New)로 동물병원 영업시간을 가져온다.
 * - searchText로 병원명+주소를 한 번에 검색하고, 필드마스크로 영업시간까지 같이 받아온다.
 * - 응답의 regularOpeningHours를 파싱해 24시간 영업/야간 진료 여부를 추정한다.
 */
@Component
public class GooglePlacesClient {
    private static final String SEARCH_TEXT_URL = "https://places.googleapis.com/v1/places:searchText";
    // 필요한 필드만 요청해야 과금/응답 크기를 줄일 수 있다.
    private static final String FIELD_MASK =
            "places.id,places.displayName,places.regularOpeningHours,places.nationalPhoneNumber,places.internationalPhoneNumber";

    private final RestClient restClient;
    private final String apiKey;

    public GooglePlacesClient(RestClient.Builder builder,
                              @Value("${google.places-api-key:}") String apiKey) {
        this.restClient = builder.build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public PlaceHours fetchHours(String name, String address) {
        if (apiKey.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        List<String> queries = searchQueries(name, address);
        PlaceHours fallback = null;
        for (String query : queries) {
            PlaceHours result = searchHours(query);
            if (result == null) {
                continue;
            }
            if (result.openingHours() != null || result.phone() != null) {
                return result;
            }
            if (fallback == null) {
                fallback = result;
            }
        }
        return fallback;
    }

    private PlaceHours searchHours(String query) {
        JsonNode body;
        try {
            body = restClient.post()
                    .uri(SEARCH_TEXT_URL)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "textQuery", query,
                            "languageCode", "ko",
                            "regionCode", "KR",
                            "maxResultCount", 5
                    ))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new GooglePlacesException("구글 Places API가 거부됐습니다. 콘솔에서 Places API(New) 사용 설정과 키 제한을 확인해주세요.");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new GooglePlacesException("구글 Places API 키가 올바르지 않습니다.");
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new GooglePlacesException("구글 Places API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        } catch (RestClientException e) {
            return null;
        }

        if (body == null || !body.has("places") || body.get("places").isEmpty()) {
            return null;
        }

        PlaceHours fallback = null;
        for (JsonNode place : body.get("places")) {
            PlaceHours parsed = parsePlaceHours(place);
            if (parsed == null) {
                continue;
            }
            if (parsed.openingHours() != null || parsed.phone() != null) {
                return parsed;
            }
            if (fallback == null) {
                fallback = parsed;
            }
        }
        return fallback;
    }

    private PlaceHours parsePlaceHours(JsonNode place) {
        JsonNode hours = place.get("regularOpeningHours");
        String phone = firstText(place, "nationalPhoneNumber", "internationalPhoneNumber");
        if (hours == null || hours.isNull()) {
            return new PlaceHours(text(place, "id"), null, phone, false, false, false);
        }

        String summary = buildSummary(hours);
        boolean open24 = detect24h(hours);
        boolean night = open24 || detectNight(hours);
        boolean weekend = open24 || detectWeekend(hours);
        if (summary == null && open24) {
            summary = "24시간 영업";
        }
        return new PlaceHours(text(place, "id"), summary, phone, open24, night, weekend);
    }

    private List<String> searchQueries(String name, String address) {
        String safeName = name.trim();
        List<String> queries = new ArrayList<>();
        if (address != null && !address.isBlank()) {
            queries.add(safeName + " " + address.trim());
        }
        queries.add(safeName);
        if (!safeName.contains("동물병원") && !safeName.contains("동물의료센터")) {
            queries.add(safeName + " 동물병원");
        }
        return queries.stream().distinct().toList();
    }

    // weekdayDescriptions(요일별 영업시간 문장)를 줄바꿈으로 합쳐 보관한다.
    private String buildSummary(JsonNode hours) {
        JsonNode descriptions = hours.get("weekdayDescriptions");
        if (descriptions == null || !descriptions.isArray() || descriptions.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode line : descriptions) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line.asText());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    // 24시간 영업: 종료 시각 없는 open 주기(구글의 24/7 표기) 또는 요일 설명에 '24' 포함.
    private boolean detect24h(JsonNode hours) {
        JsonNode periods = hours.get("periods");
        if (periods != null && periods.isArray() && periods.size() == 1) {
            JsonNode period = periods.get(0);
            if (period.has("open") && !period.has("close")) {
                return true;
            }
        }
        JsonNode descriptions = hours.get("weekdayDescriptions");
        if (descriptions != null && descriptions.isArray()) {
            for (JsonNode line : descriptions) {
                String text = line.asText();
                if (text.contains("24시간") || text.toLowerCase().contains("open 24 hours")) {
                    return true;
                }
            }
        }
        return false;
    }

    // 야간 진료: 어느 요일이든 종료 시각이 22시 이후거나 새벽(자정 넘김).
    private boolean detectNight(JsonNode hours) {
        JsonNode periods = hours.get("periods");
        if (periods == null || !periods.isArray()) {
            return false;
        }
        for (JsonNode period : periods) {
            JsonNode open = period.get("open");
            JsonNode close = period.get("close");
            if (close == null) {
                continue;
            }
            int closeHour = close.path("hour").asInt(-1);
            int openHour = open == null ? -1 : open.path("hour").asInt(-1);
            int closeDay = close.path("day").asInt(-1);
            int openDay = open == null ? -1 : open.path("day").asInt(-1);
            // 종료가 22시 이후이거나, 종료 요일이 시작 요일보다 뒤(=자정을 넘겨 영업).
            if (closeHour >= 22 || (openDay != -1 && closeDay != -1 && closeDay != openDay) ||
                    (closeHour >= 0 && closeHour <= 6 && openHour > closeHour)) {
                return true;
            }
        }
        return false;
    }

    private boolean detectWeekend(JsonNode hours) {
        JsonNode periods = hours.get("periods");
        if (periods == null || !periods.isArray()) {
            return false;
        }
        for (JsonNode period : periods) {
            JsonNode open = period.get("open");
            if (open == null) {
                continue;
            }
            int openDay = open.path("day").asInt(-1);
            if (openDay == 0 || openDay == 6) {
                return true;
            }
        }
        return false;
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public record PlaceHours(String googlePlaceId, String openingHours, String phone, boolean open24,
                             boolean nightAvailable, boolean weekendAvailable) {
    }
}
