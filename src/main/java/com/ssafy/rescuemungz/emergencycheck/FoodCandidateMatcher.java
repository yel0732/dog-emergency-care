package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import com.ssafy.rescuemungz.foodsafety.FoodSafetyMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class FoodCandidateMatcher {
    private final FoodSafetyMapper mapper;

    public FoodCandidateMatcher(FoodSafetyMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<FoodSafety> match(EmergencyCheckRequest request, StructuredSymptomInput structured) {
        if (request.suspectedFoodId() != null) {
            FoodSafety food = mapper.findById(request.suspectedFoodId());
            if (food != null) return Optional.of(food);
        }
        for (String candidate : candidates(request, structured)) {
            FoodSafety food = mapper.findByFoodNameExact(candidate);
            if (food != null) return Optional.of(food);
            FoodSafety alias = mapper.findByAliasExact(candidate);
            if (alias != null) return Optional.of(alias);
        }
        for (String candidate : candidates(request, structured)) {
            String normalizedCandidate = TextSafety.normalize(candidate);
            if (normalizedCandidate.length() < 2) continue;
            List<FoodSafety> matches = mapper.search(candidate);
            for (FoodSafety food : matches) {
                String normalizedFood = TextSafety.normalize(food.getFoodName());
                boolean contained = normalizedFood.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedFood);
                if (contained && Math.min(normalizedFood.length(), normalizedCandidate.length()) >= 2) {
                    return Optional.of(food);
                }
            }
        }
        return Optional.empty();
    }

    private Set<String> candidates(EmergencyCheckRequest request, StructuredSymptomInput structured) {
        Set<String> values = new LinkedHashSet<>();
        add(values, request.suspectedFoodText());
        if (structured != null) {
            add(values, structured.suspectedFoodText());
            if (structured.foodOrToxinKeywords() != null) {
                structured.foodOrToxinKeywords().forEach(value -> add(values, value));
            }
        }
        return values;
    }

    private void add(Set<String> values, String value) {
        String cleaned = TextSafety.clean(value, 100);
        if (cleaned != null && !cleaned.isBlank()) {
            values.add(cleaned);
        }
    }
}
