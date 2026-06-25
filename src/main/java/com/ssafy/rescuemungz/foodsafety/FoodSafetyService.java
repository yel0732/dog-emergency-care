package com.ssafy.rescuemungz.foodsafety;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.rescuemungz.common.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class FoodSafetyService {
    private final FoodSafetyMapper mapper;
    private final ObjectMapper objectMapper;

    public FoodSafetyService(FoodSafetyMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public List<FoodSafety> search(String keyword) {
        return hydrate(mapper.search(blankToNull(keyword)));
    }

    public FoodSafety find(long id) {
        FoodSafety food = mapper.findById(id);
        if (food == null) {
            throw new NotFoundException("Food safety information not found.");
        }
        hydrate(food);
        return food;
    }

    private List<FoodSafety> hydrate(List<FoodSafety> foods) {
        foods.forEach(this::hydrate);
        return foods;
    }

    private void hydrate(FoodSafety food) {
        List<String> aliases = mapper.findAliases(food.getId());
        food.setAliases(aliases);
        List<FoodSafetyReference> references = parseReferences(food.getReferencesJson());
        food.setReferenceLinks(references);
        food.setReferences(references.stream().map(FoodSafetyReference::label).toList());
        food.setTags(buildTags(food, aliases));
    }

    private List<FoodSafetyReference> parseReferences(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) return List.of();

            List<FoodSafetyReference> references = new ArrayList<>();
            for (JsonNode item : root) {
                if (item.isTextual()) {
                    addReference(references, item.asText(), "", "", "");
                    continue;
                }

                String org = text(item, "source_org");
                String title = text(item, "source_title");
                String url = firstText(item, "url", "source_url");
                String publicTitle = publicReferenceTitle(title, org, url);
                String label = referenceLabel(org, publicTitle);
                addReference(references, label, org, publicTitle, url);
            }
            return references;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> buildTags(FoodSafety food, List<String> aliases) {
        Set<String> tags = new LinkedHashSet<>();
        add(tags, food.getRiskLevel());
        if (Boolean.TRUE.equals(food.getImmediateVet())) {
            add(tags, "즉시 병원 권장");
        }
        addSymptomTags(tags, food.getObservedSymptoms());
        add(tags, firstSentence(food.getRiskCondition()));
        aliases.stream().limit(4).forEach(alias -> add(tags, alias));
        return new ArrayList<>(tags).stream().limit(10).toList();
    }

    private void addSymptomTags(Set<String> tags, String symptoms) {
        if (symptoms == null || symptoms.isBlank()) return;
        for (String symptom : symptoms.split("[,，/\\n]")) {
            String value = firstSentence(symptom).replaceFirst("^[-•\\s]+", "").trim();
            if (value.length() > 18) continue;
            add(tags, value);
        }
    }

    private String firstSentence(String value) {
        if (value == null || value.isBlank()) return "";
        String[] parts = value.split("[.!?。]");
        return parts.length == 0 ? value.trim() : parts[0].trim();
    }

    private void add(Set<String> tags, String value) {
        if (value == null || value.isBlank()) return;
        tags.add(value.trim());
    }

    private void add(List<String> values, String value) {
        if (value == null || value.isBlank()) return;
        values.add(value.trim());
    }

    private void addReference(List<FoodSafetyReference> values, String label, String org, String title, String url) {
        if (label == null || label.isBlank()) return;
        values.add(new FoodSafetyReference(label.trim(), org.trim(), title.trim(), url.trim()));
    }

    private String referenceLabel(String org, String title) {
        if (org.isBlank()) return title;
        if (title.isBlank()) return org;
        if (org.toLowerCase().contains(title.toLowerCase()) || title.toLowerCase().contains(org.toLowerCase())) return org;
        return org + " - " + title;
    }

    private boolean isInternalReferenceTitle(String title) {
        if (title == null || title.isBlank()) return false;
        return title.contains("기존 ") || title.contains("보완용") || title.contains("병합 권장") || title.contains("실제 DB 반영");
    }

    private String publicReferenceTitle(String title, String org, String url) {
        String cleanTitle = title == null ? "" : title.trim();
        String cleanOrg = org == null ? "" : org.trim();
        String inferred = inferReferenceTitle(url);
        if (isInternalReferenceTitle(cleanTitle)) return inferred;
        if (!cleanTitle.isBlank() && !cleanOrg.isBlank()
                && cleanOrg.toLowerCase().contains(cleanTitle.toLowerCase())
                && !inferred.isBlank()) {
            return inferred;
        }
        return cleanTitle;
    }

    private String inferReferenceTitle(String url) {
        String value = url == null ? "" : url.trim();
        if (value.contains("people-foods-avoid-feeding-your-pets")) return "People Foods to Avoid Feeding Your Pets";
        if (value.contains("poison-proof-your-home/kitchen")) return "Top 10 Kitchen Toxins";
        if (value.contains("onion-and-garlic-poisoning-in-dogs")) return "Onion and garlic poisoning in dogs";
        if (value.contains("can-dogs-eat-chicken")) return "Can Dogs Eat Chicken?";
        if (value.contains("can-dogs-eat-sweet-potatoes")) return "Can Dogs Eat Sweet Potatoes?";
        if (value.contains("can-dogs-eat-cucumbers")) return "Can Dogs Eat Cucumbers?";
        return "";
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = text(node, fieldName);
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
