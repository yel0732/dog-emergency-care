package com.ssafy.rescuemungz.hospital;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class KakaoLocalClient {
    private static final String ADDRESS_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    private final RestClient restClient;
    private final String restApiKey;

    public KakaoLocalClient(RestClient.Builder builder,
                            @Value("${kakao.rest-api-key:}") String restApiKey) {
        this.restClient = builder.build();
        this.restApiKey = restApiKey == null ? "" : restApiKey.trim();
    }

    public boolean isConfigured() {
        return !restApiKey.isBlank();
    }

    public KakaoCoordinate searchAddress(String address) {
        if (restApiKey.isBlank() || address == null || address.isBlank()) {
            return null;
        }

        JsonNode body;
        try {
            body = restClient.get()
                    .uri(ADDRESS_SEARCH_URL + "?query={query}", address)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new KakaoLocalException("카카오 개발자 콘솔에서 지도/로컬 서비스 사용 설정을 켜야 좌표 보정이 가능합니다.");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new KakaoLocalException("카카오 REST API 키를 다시 확인해주세요.");
        } catch (RestClientException e) {
            return null;
        }

        if (body == null || !body.has("documents") || body.get("documents").isEmpty()) {
            return null;
        }

        JsonNode first = body.get("documents").get(0);
        String x = text(first, "x");
        String y = text(first, "y");
        if (x == null || y == null) {
            return null;
        }

        String placeId = text(first, "id");
        return new KakaoCoordinate(new BigDecimal(y), new BigDecimal(x), placeId);
    }

    public KakaoPlace searchPlace(String name, String address) {
        if (restApiKey.isBlank() || name == null || name.isBlank()) {
            return null;
        }

        String query = address == null || address.isBlank() ? name.trim() : name.trim() + " " + address.trim();
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(KEYWORD_SEARCH_URL + "?query={query}", query)
                    .header("Authorization", "KakaoAK " + restApiKey)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new KakaoLocalException("카카오 개발자 콘솔에서 지도/로컬 서비스 사용 설정을 켜야 좌표 보정이 가능합니다.");
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new KakaoLocalException("카카오 REST API 키를 다시 확인해주세요.");
        } catch (RestClientException e) {
            return null;
        }

        if (body == null || !body.has("documents") || body.get("documents").isEmpty()) {
            return null;
        }

        JsonNode first = body.get("documents").get(0);
        String x = text(first, "x");
        String y = text(first, "y");
        if (x == null || y == null) {
            return null;
        }

        return new KakaoPlace(
                new BigDecimal(y),
                new BigDecimal(x),
                text(first, "id"),
                text(first, "phone")
        );
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }

    public record KakaoCoordinate(BigDecimal lat, BigDecimal lng, String placeId) {
    }

    public record KakaoPlace(BigDecimal lat, BigDecimal lng, String placeId, String phone) {
    }
}
