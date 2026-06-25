package com.ssafy.rescuemungz.foodsafety;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FoodSafetySeedCompatibility implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(FoodSafetySeedCompatibility.class);

    private final JdbcTemplate jdbcTemplate;

    public FoodSafetySeedCompatibility(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            upsertReviewFoodSeeds();
        } catch (Exception e) {
            log.warn("음식 안전 보강 데이터를 반영하지 못했습니다. schema.sql/data.sql 재적용 여부를 확인해 주세요.", e);
        }
    }

    private void upsertReviewFoodSeeds() {
        upsertFood(
                "닭가슴살",
                "안전",
                "양념 없이 충분히 익힌 닭가슴살은 일반적으로 반려견에게 급여할 수 있는 단백질 식품입니다.",
                "대부분 특별한 증상 없음. 과식하거나 기존 소화기 질환이 있으면 구토, 설사, 복부 불편이 생길 수 있음",
                "껍질, 뼈, 양념, 소스, 기름을 제거하고 소량씩 급여합니다. 구토나 설사가 반복되면 급여를 중단하고 병원에 문의합니다.",
                "처음 먹는 경우에는 소량부터 확인하고, 알레르기 병력이나 췌장염 병력이 있으면 수의사 상담 후 급여합니다.",
                "삶거나 찐 무양념 닭가슴살 기준. 양파, 마늘, 소금, 버터, 매운 소스가 들어간 조리식은 제외",
                "[{\"source_org\":\"AKC\",\"source_title\":\"Can Dogs Eat Chicken?\",\"url\":\"https://www.akc.org/expert-advice/nutrition/can-dogs-eat-chicken/\"}]",
                false
        );
        aliases("닭가슴살", "닭 가슴살", "치킨", "삶은 닭가슴살", "chicken breast");

        upsertFood(
                "고구마",
                "안전",
                "익힌 고구마는 소량 급여 시 섬유질과 탄수화물 공급원이 될 수 있습니다.",
                "대부분 특별한 증상 없음. 과식하면 설사, 복부 팽만, 구토가 생길 수 있음",
                "삶거나 찐 고구마를 식혀서 껍질과 양념 없이 소량 급여합니다. 소화기 증상이 있으면 급여를 중단합니다.",
                "당뇨, 비만, 소화기 질환이 있거나 처음 급여하는 경우에는 양을 줄이고 상태를 관찰합니다.",
                "익힌 무양념 고구마 기준. 생고구마, 설탕/버터/소금이 들어간 조리식은 제외",
                "[{\"source_org\":\"AKC\",\"source_title\":\"Can Dogs Eat Sweet Potatoes?\",\"url\":\"https://www.akc.org/expert-advice/nutrition/can-dogs-eat-sweet-potatoes/\"}]",
                false
        );
        aliases("고구마", "찐고구마", "삶은 고구마", "sweet potato");

        upsertFood(
                "오이",
                "안전",
                "오이는 수분이 많은 저칼로리 간식으로 소량 급여할 수 있습니다.",
                "대부분 특별한 증상 없음. 과식하거나 크게 삼키면 구토, 설사, 목 걸림이 생길 수 있음",
                "깨끗이 씻어 작은 조각으로 잘라 소량 급여합니다. 피클처럼 소금이나 양념이 들어간 제품은 피합니다.",
                "삼킴이 급한 반려견은 작게 잘라 주고, 소화기 증상이 있으면 급여를 중단합니다.",
                "생오이 소량 기준. 피클, 소금 절임, 양념 오이는 제외",
                "[{\"source_org\":\"AKC\",\"source_title\":\"Can Dogs Eat Cucumbers?\",\"url\":\"https://www.akc.org/expert-advice/nutrition/can-dogs-eat-cucumbers/\"}]",
                false
        );
        aliases("오이", "cucumber", "생오이", "오이 간식");

        upsertFood("우유/유제품", "주의",
                "반려동물은 락타아제가 충분하지 않아 우유와 유제품 섭취 시 설사 등 소화기 증상이 생길 수 있습니다.",
                "설사, 복부 불편, 구토 가능",
                "소량 섭취 후 상태를 관찰하고 반복 설사/구토 시 병원에 문의합니다.",
                "섭취량과 유당 함량, 기존 소화기 상태에 따라 달라질 수 있습니다.",
                "우유/유제품 섭취",
                "[{\"source_org\":\"ASPCA Animal Poison Control\",\"source_title\":\"People Foods to Avoid Feeding Your Pets\",\"url\":\"https://www.aspca.org/pet-care/aspca-poison-control/people-foods-avoid-feeding-your-pets\"}]",
                false);
        aliases("우유/유제품", "우유", "유제품", "치즈", "요거트");

        upsertFood("일반 견과류/고지방 음식", "주의",
                "고지방 음식은 소화기 불편과 췌장 부담을 일으킬 수 있고, 일부 견과류는 중독 위험이 있습니다.",
                "구토, 설사, 복부 통증, 무기력 가능",
                "먹은 종류와 양을 확인하고 증상이 반복되면 병원에 문의합니다.",
                "견과류 종류, 지방 함량, 섭취량에 따라 위험도가 달라집니다.",
                "견과류 또는 기름진 음식 섭취",
                "[{\"source_org\":\"ASPCA Animal Poison Control\",\"source_title\":\"People Foods to Avoid Feeding Your Pets\",\"url\":\"https://www.aspca.org/pet-care/aspca-poison-control/people-foods-avoid-feeding-your-pets\"}]",
                false);
        aliases("일반 견과류/고지방 음식", "견과류", "아몬드", "호두", "피칸", "고지방 음식", "기름진 음식");

        upsertFood("생고기/날계란", "주의",
                "생고기와 날계란은 세균 오염이나 소화기 감염 위험이 있어 급여에 주의가 필요합니다.",
                "구토, 설사, 복통, 식욕저하 가능",
                "섭취량과 시간을 확인하고 구토/설사/무기력이 있으면 병원에 문의합니다.",
                "어린 강아지, 노령견, 면역 저하 반려견은 더 주의합니다.",
                "익히지 않은 육류 또는 날계란 섭취",
                "[{\"source_org\":\"AVMA\",\"source_title\":\"Raw or Undercooked Animal-Source Protein\",\"url\":\"https://www.avma.org/resources-tools/avma-policies/raw-or-undercooked-animal-source-protein-cat-and-dog-diets\"}]",
                false);
        aliases("생고기/날계란", "생고기", "날고기", "날계란", "생달걀", "raw meat", "raw egg");

        upsertFood("생감자/날감자", "주의",
                "익히지 않은 감자나 싹난 감자는 반려견에게 소화기 불편과 독성 위험을 줄 수 있습니다.",
                "구토, 설사, 복부 불편 가능",
                "먹은 양과 상태를 확인하고 소화기 증상이 있으면 병원에 문의합니다.",
                "익히지 않은 감자, 싹난 감자, 양념된 감자는 피합니다.",
                "생감자/날감자 섭취",
                "[{\"source_org\":\"PDSA\",\"source_title\":\"Onion and garlic poisoning in dogs\",\"url\":\"https://www.pdsa.org.uk/pet-help-and-advice/pet-health-hub/conditions/onion-and-garlic-poisoning-in-dogs\"}]",
                false);
        aliases("생감자/날감자", "생감자", "날감자", "싹난 감자", "raw potato");

        upsertFood("필로덴드론/스네이크플랜트/포토스", "주의",
                "칼슘옥살레이트 결정이 입안 자극, 침흘림, 구토를 유발할 수 있습니다.",
                "입 통증, 침흘림, 구토",
                "식물명과 섭취량을 확인하고 증상이 있으면 병원에 상담합니다.",
                "잎을 씹거나 섭취한 경우",
                "필로덴드론, 스네이크플랜트, 포토스 섭취",
                "[{\"source_org\":\"VCA Animal Hospitals\",\"source_title\":\"Top 10 Toxic Household Plants For Pets\",\"url\":\"https://vcahospitals.com/resources/lifestyle-cat/hazards-safety/top-10-toxic-household-plants-for-pets\"}]",
                false);
        aliases("필로덴드론/스네이크플랜트/포토스", "필로덴드론", "스네이크플랜트", "포토스", "몬스테라");

        upsertFood("알로에", "주의",
                "알로에 섭취는 구토, 설사, 무기력 등 소화기 증상을 유발할 수 있습니다.",
                "구토, 설사, 무기력, 식욕저하",
                "섭취량과 증상을 확인하고 반복 증상이 있으면 병원에 문의합니다.",
                "잎 또는 겔 섭취 여부를 확인합니다.",
                "알로에 섭취",
                "[{\"source_org\":\"VCA Animal Hospitals\",\"source_title\":\"Top 10 Toxic Household Plants For Pets\",\"url\":\"https://vcahospitals.com/resources/lifestyle-cat/hazards-safety/top-10-toxic-household-plants-for-pets\"}]",
                false);
        aliases("알로에", "aloe", "알로에베라", "알로에 베라");

        upsertFood("국화", "주의",
                "국화류 식물은 섭취 시 구토, 설사, 침흘림, 피부 자극을 일으킬 수 있습니다.",
                "구토, 설사, 침흘림, 피부 자극",
                "식물명과 섭취량을 확인하고 증상이 있으면 병원에 상담합니다.",
                "꽃, 잎, 줄기 섭취 여부를 확인합니다.",
                "국화 섭취",
                "[{\"source_org\":\"VCA Animal Hospitals\",\"source_title\":\"Top 10 Toxic Household Plants For Pets\",\"url\":\"https://vcahospitals.com/resources/lifestyle-cat/hazards-safety/top-10-toxic-household-plants-for-pets\"}]",
                false);
        aliases("국화", "chrysanthemum", "국화꽃");

        upsertFood("옥수/Jade plant", "주의",
                "옥수 식물은 씹거나 섭취하면 구토, 우울, 균형 상실이 나타날 수 있습니다.",
                "구토, 우울, 균형 상실",
                "섭취량과 신경 증상 여부를 확인하고 병원에 상담합니다.",
                "옥수 식물 섭취",
                "옥수 식물 섭취",
                "[{\"source_org\":\"VCA Animal Hospitals\",\"source_title\":\"Top 10 Toxic Household Plants For Pets\",\"url\":\"https://vcahospitals.com/resources/lifestyle-cat/hazards-safety/top-10-toxic-household-plants-for-pets\"}]",
                false);
        aliases("옥수/Jade plant", "옥수", "염좌", "jade plant", "크라슐라");

        upsertFood("아스파라거스 펀", "주의",
                "아스파라거스 펀은 접촉 시 피부 자극, 열매 섭취 시 구토·복통·설사를 유발할 수 있습니다.",
                "피부 자극, 구토, 복통, 설사",
                "접촉/섭취 여부와 증상을 확인하고 병원에 상담합니다.",
                "식물 접촉 또는 열매 섭취",
                "식물 접촉 또는 열매 섭취",
                "[{\"source_org\":\"VCA Animal Hospitals\",\"source_title\":\"Top 10 Toxic Household Plants For Pets\",\"url\":\"https://vcahospitals.com/resources/lifestyle-cat/hazards-safety/top-10-toxic-household-plants-for-pets\"}]",
                false);
        aliases("아스파라거스 펀", "asparagus fern", "아스파라거스펀");
    }

    private void upsertFood(String foodName,
                            String riskLevel,
                            String dangerReason,
                            String observedSymptoms,
                            String response,
                            String doseNote,
                            String riskCondition,
                            String sourceReferences,
                            boolean immediateVet) {
        jdbcTemplate.update("""
                INSERT INTO food_safety
                    (food_name, risk_level, danger_reason, observed_symptoms, response, dose_note,
                     risk_condition, source_references, is_immediate_vet)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    risk_level = VALUES(risk_level),
                    danger_reason = VALUES(danger_reason),
                    observed_symptoms = VALUES(observed_symptoms),
                    response = VALUES(response),
                    dose_note = VALUES(dose_note),
                    risk_condition = VALUES(risk_condition),
                    source_references = VALUES(source_references),
                    is_immediate_vet = VALUES(is_immediate_vet)
                """,
                foodName, riskLevel, dangerReason, observedSymptoms, response, doseNote,
                riskCondition, sourceReferences, immediateVet);
    }

    private void aliases(String foodName, String... aliases) {
        for (String alias : aliases) {
            jdbcTemplate.update("""
                    INSERT INTO food_aliases (food_id, alias_name)
                    SELECT id, ? FROM food_safety WHERE food_name = ?
                    ON DUPLICATE KEY UPDATE food_id = VALUES(food_id)
                    """, alias, foodName);
        }
    }
}
