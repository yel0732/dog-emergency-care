package com.ssafy.rescuemungz.emergencycheck;

import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import com.ssafy.rescuemungz.foodsafety.FoodSafetyMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodCandidateMatcherTests {

    @Test
    void exactFoodNameWinsOverAliasCollision() {
        FoodCandidateMatcher matcher = new FoodCandidateMatcher(new FakeFoodSafetyMapper());
        EmergencyCheckRequest request = new EmergencyCheckRequest(
                1L,
                null,
                null,
                null,
                null,
                null,
                "초콜릿",
                null,
                "아직 특별한 증상은 없지만 먹은 게 걱정돼요.",
                List.of(),
                List.of(),
                List.of()
        );

        FoodSafety food = matcher.match(request, StructuredSymptomInput.empty(request.symptomNote())).orElseThrow();

        assertThat(food.getFoodName()).isEqualTo("초콜릿");
    }

    private static class FakeFoodSafetyMapper implements FoodSafetyMapper {
        @Override
        public List<FoodSafety> search(String keyword) {
            return List.of(food("초콜릿"));
        }

        @Override
        public FoodSafety findById(long id) {
            return null;
        }

        @Override
        public FoodSafety findByFoodNameExact(String foodName) {
            return "초콜릿".equals(foodName) ? food("초콜릿") : null;
        }

        @Override
        public FoodSafety findByAliasExact(String aliasName) {
            return "초콜릿".equals(aliasName) ? food("할로윈 사탕") : null;
        }

        @Override
        public List<String> findAliases(long foodId) {
            return List.of();
        }

        private static FoodSafety food(String name) {
            FoodSafety food = new FoodSafety();
            food.setFoodName(name);
            food.setRiskLevel("위험");
            food.setImmediateVet(true);
            return food;
        }
    }
}
