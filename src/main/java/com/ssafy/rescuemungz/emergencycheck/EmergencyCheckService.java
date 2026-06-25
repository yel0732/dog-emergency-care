package com.ssafy.rescuemungz.emergencycheck;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.ssafy.rescuemungz.common.NotFoundException;
import com.ssafy.rescuemungz.foodsafety.FoodSafety;
import com.ssafy.rescuemungz.pet.Pet;
import com.ssafy.rescuemungz.pet.PetService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class EmergencyCheckService {
    private final EmergencyCheckMapper checkMapper;
    private final VetReportMapper reportMapper;
    private final EmergencyCheckEvidenceMapper checkEvidenceMapper;
    private final EmergencyAiClient aiClient;
    private final PetService petService;
    private final FoodCandidateMatcher foodMatcher;
    private final EmergencyRuleMatcher ruleMatcher;
    private final EmergencyRiskResolver riskResolver;
    private final EmergencyGuideFactory guideFactory;
    private final ObjectMapper objectMapper;

    public EmergencyCheckService(EmergencyCheckMapper checkMapper, VetReportMapper reportMapper,
                                 EmergencyCheckEvidenceMapper checkEvidenceMapper, EmergencyAiClient aiClient,
                                 PetService petService, FoodCandidateMatcher foodMatcher,
                                 EmergencyRuleMatcher ruleMatcher, EmergencyRiskResolver riskResolver,
                                 EmergencyGuideFactory guideFactory, ObjectMapper objectMapper) {
        this.checkMapper = checkMapper;
        this.reportMapper = reportMapper;
        this.checkEvidenceMapper = checkEvidenceMapper;
        this.aiClient = aiClient;
        this.petService = petService;
        this.foodMatcher = foodMatcher;
        this.ruleMatcher = ruleMatcher;
        this.riskResolver = riskResolver;
        this.guideFactory = guideFactory;
        this.objectMapper = objectMapper;
    }

    public EmergencyCheck create(long userId, EmergencyCheckRequest request) {
        Pet pet = petService.find(request.petId(), userId);
        StructuredSymptomInput structured = aiClient.structure(request)
                .orElseGet(() -> StructuredSymptomInput.empty(request.symptomNote()));
        Optional<FoodSafety> matchedFood = foodMatcher.match(request, structured);
        FoodSafety food = matchedFood.orElse(null);
        List<MatchedEmergencyRule> matches = prioritizeMatches(request, ruleMatcher.match(request, structured, food));
        String riskLevel = riskResolver.resolveRisk(request, food, matches, structured);
        boolean immediateVet = riskResolver.immediateVet(riskLevel, food, matches);
        AvailableActions actions = availableActions(request, riskLevel, matches);
        EmergencyGuideResponse guide = "정보부족".equals(riskLevel)
                ? guideFactory.fallback(riskLevel, riskResolver.reason(riskLevel, food, matches), food, matches, actions, true)
                : aiClient.guide(request, pet, food, riskLevel, immediateVet, structured, matches, actions)
                .orElseGet(() -> guideFactory.fallback(riskLevel, riskResolver.reason(riskLevel, food, matches), food, matches, actions, true));
        if (!"정보부족".equals(riskLevel)) {
            guide = normalizeVetQuestions(riskLevel, request, structured, food, matches, guide);
        }
        if ("정보부족".equals(riskLevel)) {
            return transientCheck(userId, request, food, structured, riskLevel, guide, actions);
        }

        EmergencyCheck check = new EmergencyCheck();
        check.setUserId(userId);
        check.setPetId(request.petId());
        check.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        check.setRepeatCount(request.repeatCount() == null ? 0 : request.repeatCount());
        check.setSuspectedFoodId(food == null ? request.suspectedFoodId() : food.getId());
        check.setSuspectedFoodText(blankToNull(request.suspectedFoodText()));
        check.setSymptomNote(request.symptomNote());
        check.setSymptomTags(toJson(request.symptomTags()));
        check.setStructuredInput(toJson(structured));
        check.setPhotoUrls(toJson(request.photoUrls()));
        check.setRiskLevel(riskLevel);
        check.setRiskReason(riskResolver.reason(riskLevel, food, matches));
        check.setRecommendedAction(riskResolver.recommendedAction(riskLevel, matches));
        check.setAnalysisResult(guardianSummary(riskLevel, request, structured, guide, matches, food));
        check.setImmediateVet(immediateVet);
        checkMapper.insert(check);
        for (MatchedEmergencyRule match : matches) {
            checkEvidenceMapper.insert(check.getId(), match.evidence().getId(), match.rule().getId(),
                    match.score(), toJson(match.matchedKeywords()));
        }

        VetReport report = new VetReport();
        report.setCheckId(check.getId());
        report.setRiskSummary(vetRiskSummary(check, request, structured, guide, matches, food));
        report.setSituationSummary(structured.originalSummary() == null ? request.symptomNote() : structured.originalSummary());
        report.setImmediateActions(toJson(guide.immediateActions()));
        report.setAvoidActions(toJson(guide.avoidActions()));
        report.setObservationChecklist(toJson(guide.observationChecklist()));
        report.setEscalationCriteria(toJson(guide.escalationCriteria()));
        report.setHospitalMessage(toJson(Map.of("immediateVet", immediateVet, "message",
                immediateVet ? "즉시 동물병원에 연락해 주세요." : "악화 시 동물병원에 연락해 주세요.")));
        report.setOptionalQuestions(toJson(guide.optionalQuestions()));
        report.setEvidenceSummary(toJson(guide.evidenceSummary()));
        report.setLlmResponseJson(toJson(guide));
        report.setPetSnapshot(petSnapshot(pet));
        report.setSymptomSnapshot("Occurred: %s\nSymptoms: %s\nTags: %s\nSuspected food: %s"
                .formatted(check.getOccurredAt(), request.symptomNote(), request.symptomTags(),
                        food == null ? request.suspectedFoodText() : food.getFoodName()));
        reportMapper.insert(report);
        EmergencyCheck saved = find(check.getId(), userId);
        applyGuide(saved, guide);
        return saved;
    }

    public List<EmergencyCheck> findMine(long userId) {
        return checkMapper.findByUserId(userId);
    }

    public EmergencyCheck find(long id, long userId) {
        EmergencyCheck check = checkMapper.findByIdAndUserId(id, userId);
        if (check == null) {
            throw new NotFoundException("Emergency check not found.");
        }
        return check;
    }

    public void delete(long id, long userId) {
        if (checkMapper.delete(id, userId) == 0) {
            throw new NotFoundException("Emergency check not found.");
        }
    }

    public VetReport findVetReport(long checkId, long userId) {
        EmergencyCheck check = find(checkId, userId);
        VetReport report = reportMapper.findByCheckId(checkId);
        if (report == null) {
            throw new NotFoundException("Vet report not found.");
        }
        return normalizeVetReportQuestions(report, check.getRiskLevel());
    }

    public byte[] vetReportPdf(long checkId, long userId) {
        EmergencyCheck check = find(checkId, userId);
        VetReport report = findVetReport(checkId, userId);
        String html = """
                <html><head><meta charset="UTF-8"/>
                <style>
                @page{margin:18mm 15mm}
                body{font-family:'Malgun Gothic',Arial,sans-serif;color:#17211f;line-height:1.58;margin:0;padding:0;font-size:13px;font-weight:400}
                h1,h2{margin:0;color:#17211f;font-weight:600;letter-spacing:0}
                h1{font-size:23px;line-height:1.25}
                h2{font-size:15px;line-height:1.35}
                p{margin:0}
                .report-head{display:table;width:100%%;margin-bottom:12px;border-bottom:1px solid #dfe8e4;padding-bottom:12px}
                .report-head-main{display:table-cell;vertical-align:middle}
                .report-head-main p{margin-top:5px;color:#66756f;font-size:12px}
                .report-head-risk{display:table-cell;width:1%%;vertical-align:middle;text-align:right;white-space:nowrap}
                .badge{display:inline-block;padding:6px 11px;border-radius:20px;color:white;font-size:12px;font-weight:600}
                .badge.urgent{background:#dc3f3a}.badge.caution{background:#d97706}.badge.observe{background:#178253}.badge.safe{background:#475569}
                .top-row{width:100%%;border-collapse:separate;border-spacing:0 0;margin-top:10px;page-break-inside:avoid}
                .top-card{width:49%%;vertical-align:top;border:1px solid #dfe8e4;border-radius:8px;padding:12px 14px;background:#fff}
                .top-gap{width:2%%;min-width:8px;border:0;padding:0}
                .box{border:1px solid #dfe8e4;border-radius:8px;padding:12px 14px;margin-top:10px;background:#fff}
                .action-box,.question-box,.source-box,.photo-box{page-break-inside:avoid}
                .box h2,.top-card h2{margin:0 0 8px 0}
                .box p,.top-card p{color:#273b36;font-size:13px;line-height:1.6}
                .meta-lines{font-size:12px!important;line-height:1.5!important;color:#3d4f49!important}
                .symptom-text{font-size:13.4px!important;line-height:1.62!important}
                .summary-box{border-color:#cddbd7;background:#fbfdfd;page-break-inside:auto}
                .summary-list{margin:4px 0 0 0}
                .summary-row{margin:0;padding:8px 0;border-top:1px solid #e7efec}
                .summary-row:first-child{border-top:0;padding-top:0}
                .summary-row dt{font-weight:600;color:#2f3f3b;margin-bottom:3px;font-size:12.5px;line-height:1.4}
                .summary-row dd{margin:0;color:#243b36;font-size:13.6px;line-height:1.62}
                .photo-grid{display:block;margin-top:8px}
                .photo-grid img{display:inline-block;width:48%%;height:auto;max-height:220px;margin:0 1%% 8px 0;vertical-align:top;border:1px solid #dfe8e4;border-radius:8px}
                .source-list li{margin-bottom:8px}
                .source-title{display:block;font-size:13px;font-weight:600;color:#263d3a;text-decoration:none}
                .source-list small{display:block;color:#66756f}
                ul{margin:8px 0 0 0;padding-left:18px}
                li{margin:4px 0;color:#273b36;font-size:13px;line-height:1.6}
                </style></head><body>
                <div class="report-head">
                  <div class="report-head-main">
                    <h1>수의사 전달 리포트</h1>
                    <p>%s · %s</p>
                  </div>
                  <div class="report-head-risk"><span class="badge %s">%s</span></div>
                </div>
                <table class="top-row"><tr>
                  <td class="top-card"><h2>반려견 정보</h2><p class="meta-lines">%s</p></td>
                  <td class="top-gap"></td>
                  <td class="top-card"><h2>보호자 입력 증상</h2><p class="symptom-text">%s</p></td>
                </tr></table>
                <div class="box summary-box"><h2>위험 요약</h2>%s</div>
                %s
                %s
                <div class="box question-box"><h2>질문</h2>%s</div>
                %s
                </body></html>
                """.formatted(escape(check.getPetName() == null ? "반려견 미선택" : check.getPetName()),
                escape(check.getCreatedAt() == null ? "작성 일시 미기록" : check.getCreatedAt().toString().replace("T", " ")),
                riskCssClass(check.getRiskLevel()), escape(check.getRiskLevel()),
                escape(petReportText(report.getPetSnapshot())).replace("\n", "<br/>"),
                escape(check.getSymptomNote()), pdfRiskSummaryHtml(check, report),
                recommendedActionsSectionHtml(check, report),
                photoSectionHtml(check.getPhotoUrls()),
                listHtml(report.getVetQuestions(), null),
                evidenceSourceHtml(report.getEvidenceSummary()));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            koreanFonts().forEach(font -> builder.useFont(font.toFile(), "Malgun Gothic"));
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("PDF 생성에 실패했습니다.", ex);
        }
    }

    private String toJson(List<String> value) {
        if (value == null || value.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private String toJson(Object value) {
        if (value == null) return "null";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private AvailableActions availableActions(EmergencyCheckRequest request, String riskLevel, List<MatchedEmergencyRule> matches) {
        String category = videoCategory(request, matches);
        boolean showHospital = !"정보부족".equals(riskLevel);
        boolean showVideo = category != null && !"정보부족".equals(riskLevel);
        String warning = "위험".equals(riskLevel)
                ? "응급 상황에서는 즉시 병원으로 이동하면서 가능한 범위의 응급처치를 진행하세요. 이동 중 보호자가 직접 하기 어려운 처치는 무리하지 말고 병원 안내를 우선해 주세요."
                : null;
        return new AvailableActions(showHospital, showVideo, category, null, warning);
    }

    private EmergencyCheck transientCheck(long userId, EmergencyCheckRequest request, FoodSafety food, StructuredSymptomInput structured,
                                          String riskLevel, EmergencyGuideResponse guide, AvailableActions actions) {
        EmergencyCheck check = new EmergencyCheck();
        check.setUserId(userId);
        check.setPetId(request.petId());
        check.setOccurredAt(request.occurredAt() == null ? LocalDateTime.now() : request.occurredAt());
        check.setRepeatCount(request.repeatCount() == null ? 0 : request.repeatCount());
        check.setSuspectedFoodId(food == null ? request.suspectedFoodId() : food.getId());
        check.setSuspectedFoodText(blankToNull(request.suspectedFoodText()));
        check.setSymptomNote(request.symptomNote());
        check.setSymptomTags(toJson(request.symptomTags()));
        check.setStructuredInput(toJson(structured));
        check.setPhotoUrls(toJson(request.photoUrls()));
        check.setRiskLevel(riskLevel);
        check.setRiskReason("현재 입력만으로는 응급도 판단에 필요한 핵심 정보가 부족합니다. 아래 질문에 답을 덧붙여 다시 확인해 주세요.");
        check.setRecommendedAction(riskResolver.recommendedAction(riskLevel, List.of()));
        check.setAnalysisResult(guardianSummary(riskLevel, request, structured, guide, List.of(), food));
        check.setImmediateVet(false);
        applyGuide(check, new EmergencyGuideResponse(
                guide.emergencyLevel(),
                guide.headline(),
                guide.immediateActions(),
                guide.avoidActions(),
                guide.observationChecklist(),
                guide.escalationCriteria(),
                guide.optionalQuestions(),
                guide.evidenceSummary(),
                guide.disclaimer(),
                actions,
                guide.fallbackUsed()
        ));
        return check;
    }

    private EmergencyGuideResponse normalizeVetQuestions(String riskLevel, EmergencyCheckRequest request,
                                                        StructuredSymptomInput structured, FoodSafety food,
                                                        List<MatchedEmergencyRule> matches,
                                                        EmergencyGuideResponse guide) {
        return new EmergencyGuideResponse(
                guide.emergencyLevel(),
                guide.headline(),
                guide.immediateActions(),
                guide.avoidActions(),
                guide.observationChecklist(),
                guide.escalationCriteria(),
                guardianVetQuestions(riskLevel, request, structured, food, matches),
                guide.evidenceSummary(),
                guide.disclaimer(),
                guide.availableActions(),
                guide.fallbackUsed()
        );
    }

    private VetReport normalizeVetReportQuestions(VetReport report, String riskLevel) {
        List<String> questions = readStringList(report.getVetQuestions());
        if (questions.isEmpty()) {
            report.setVetQuestions(toJson(EmergencyGuideFactory.guardianVetQuestions(riskLevel)));
        }
        return report;
    }

    private List<String> guardianVetQuestions(String riskLevel, EmergencyCheckRequest request, StructuredSymptomInput structured,
                                              FoodSafety food, List<MatchedEmergencyRule> matches) {
        LinkedHashSet<String> questions = new LinkedHashSet<>();
        boolean danger = "위험".equals(riskLevel);
        questions.add(danger
                ? "현재 입력한 증상 기준으로 지금 바로 응급 진료를 받아야 하나요?"
                : "현재 상태를 집에서 관찰해도 되는지, 병원 문의나 내원이 필요한 기준은 무엇인가요?");

        String directText = TextSafety.normalize(String.join(" ",
                safe(request.symptomNote()),
                safe(request.suspectedFoodText()),
                safe(request.exposureAmount()),
                request.symptomTags() == null ? "" : String.join(" ", request.symptomTags()),
                request.redFlags() == null ? "" : String.join(" ", request.redFlags())
        ));
        if (containsAnyNormalized(directText, "포도", "건포도", "초콜릿", "코코아", "카페인", "자일리톨", "양파", "마늘", "술", "알코올", "약", "진통제", "수면제", "감기약", "쥐약", "살충제", "세제", "락스", "비료", "제초제", "전자담배", "중독", "독성")
                || (food != null && !"정보부족".equals(food.getRiskLevel()) && !"안전".equals(food.getRiskLevel()))) {
            questions.add("반려견 체중과 섭취량, 섭취 후 경과 시간을 기준으로 독성 확인이나 해독 처치가 필요한가요?");
            questions.add("제품 포장지, 성분표, 남은 음식이나 구토물을 병원에 가져가야 하나요?");
        }
        if (containsAnyNormalized(directText, "이물", "삼킴", "삼켰", "뼈", "가시", "꼬치", "양말", "장난감", "비닐", "머리끈", "돌", "옥수수대", "아보카도씨", "폐색")) {
            questions.add("삼킨 물체의 크기와 모양을 봤을 때 폐색이나 장 손상 확인 검사가 필요한가요?");
            questions.add("구토를 유도하거나 음식을 먹여 내려보내는 행동을 피해야 하나요?");
        }
        if (containsAnyNormalized(directText, "구토", "토했", "설사", "혈변", "검은변")) {
            questions.add("구토물이나 변에 피가 섞여 있거나 검은 변이 보이면 어떤 검사가 필요한가요?");
        }
        if (containsAnyNormalized(directText, "호흡곤란", "숨", "헥헥", "기침", "파란잇몸", "혀가파래", "창백한잇몸", "하얀잇몸")) {
            questions.add("호흡수, 잇몸색, 산소 상태를 바로 확인해야 하는 응급 신호가 있나요?");
        }
        if (containsAnyNormalized(directText, "경련", "발작", "비틀", "실신", "쓰러", "의식", "빙빙", "고개가기울", "균형", "뒷다리")) {
            questions.add("경련이나 신경계 문제 가능성을 확인하기 위해 영상, 지속 시간, 회복 양상을 어떻게 전달하면 좋을까요?");
        }
        if (containsAnyNormalized(directText, "복부팽만", "배가빵빵", "헛구역질", "토하려는데안나", "gdv", "위확장", "위염전")) {
            questions.add("복부팽만과 헛구역질이 위확장·위염전 의심 소견인지 즉시 확인해야 하나요?");
        }
        if (containsAnyNormalized(directText, "외상", "교통사고", "자전거", "추락", "물림", "상처", "출혈", "절뚝", "발바닥", "발톱", "화상")) {
            questions.add("겉상처가 작아 보여도 골절, 내부 손상, 감염 위험 확인이 필요한가요?");
        }
        if (containsAnyNormalized(directText, "눈", "안과", "못뜨", "비벼", "각막")) {
            questions.add("눈을 못 뜨거나 비비는 증상이 각막 손상 가능성이 있어 당일 진료가 필요한가요?");
        }
        if (containsAnyNormalized(directText, "더위", "차안", "고열", "몸이뜨거", "열사병", "추위", "몸이차갑", "저체온")) {
            questions.add("체온 문제 가능성이 있는지, 병원 이동 전 체온을 어떻게 낮추거나 보온해야 하나요?");
        }
        if (request.photoUrls() != null && !request.photoUrls().isEmpty()) {
            questions.add("첨부한 사진에서 진료 판단에 참고할 만한 부분이 있는지 확인해 주실 수 있나요?");
        }
        if (request.exposureAmount() == null || request.exposureAmount().isBlank()) {
            questions.add("섭취량이나 노출량을 정확히 모를 때 어떤 정보부터 확인해 전달하면 좋을까요?");
        }

        questions.add("집으로 돌아간 뒤 어떤 변화가 생기면 즉시 다시 연락하거나 내원해야 하나요?");
        return questions.stream().limit(5).toList();
    }

    private boolean containsAnyNormalized(String value, String... needles) {
        if (value == null) return false;
        for (String needle : needles) {
            if (value.contains(TextSafety.normalize(needle))) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String videoCategory(EmergencyCheckRequest request, List<MatchedEmergencyRule> matches) {
        if (hasChokingSignal(request)) return "기도폐쇄/하임리히";
        List<String> categories = new ArrayList<>();
        matches.forEach(match -> {
            categories.add(match.rule().getCategory());
            categories.add(match.evidence().getCategory());
            categories.addAll(match.matchedKeywords());
        });
        String text = String.join(" ", categories);
        if (containsAny(text, "기도", "하임리히", "목", "이물", "삼킴", "삼켰", "먹었을 때 응급처치")) return "기도폐쇄/하임리히";
        if (containsAny(text, "초콜릿", "초코", "코코아", "카카오", "중독", "독성", "음식", "포도", "자일리톨", "양파")) return "음식주의/중독";
        if (containsAny(text, "호흡", "기도", "숨")) return "호흡기 증상";
        if (containsAny(text, "발작", "경련")) return "발작/경련";
        if (containsAny(text, "설사", "구토", "소화")) return "구토/설사/소화기 증상";
        if (containsAny(text, "심폐", "CPR", "사정지")) return "CPR/심폐소생술";
        if (containsAny(text, "위험", "응급")) return "응급상황 대처";
        return null;
    }

    private List<MatchedEmergencyRule> prioritizeMatches(EmergencyCheckRequest request, List<MatchedEmergencyRule> matches) {
        if (!hasChokingSignal(request) || matches == null || matches.isEmpty()) return matches;
        List<MatchedEmergencyRule> sorted = new ArrayList<>(matches);
        sorted.sort((left, right) -> Boolean.compare(!isChokingMatch(left), !isChokingMatch(right)));
        return sorted;
    }

    private boolean hasChokingSignal(EmergencyCheckRequest request) {
        if (request == null) return false;
        List<String> values = new ArrayList<>();
        values.add(request.symptomNote());
        values.add(request.suspectedFoodText());
        values.add(request.exposureAmount());
        if (request.symptomTags() != null) values.addAll(request.symptomTags());
        if (request.redFlags() != null) values.addAll(request.redFlags());
        String text = TextSafety.normalize(String.join(" ", values.stream().filter(v -> v != null && !v.isBlank()).toList()));
        return containsAny(text, "하임리히", "기도", "기도폐쇄", "질식", "목에", "목", "걸렸", "걸린", "켁켁", "캑캑", "컥컥", "숨", "숨쉬기힘들", "이물", "삼켰", "삼킴");
    }

    private boolean isChokingMatch(MatchedEmergencyRule match) {
        if (match == null) return false;
        String text = String.join(" ",
                safe(match.rule().getCategory()),
                safe(match.evidence().getCategory()),
                safe(match.evidence().getConditionText()),
                safe(match.evidence().getActionText()),
                String.join(" ", match.matchedKeywords()));
        return containsAny(text, "하임리히", "기도", "기도폐쇄", "질식", "목", "이물", "삼킴", "삼켰", "켁켁", "캑캑", "컥컥", "숨", "먹었을 때 응급처치");
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null) return false;
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private void applyGuide(EmergencyCheck check, EmergencyGuideResponse guide) {
        AvailableActions actions = new AvailableActions(
                guide.availableActions().showHospitalButton(),
                guide.availableActions().showVideoButton(),
                guide.availableActions().videoCategoryCode(),
                check.getId(),
                guide.availableActions().warningMessage()
        );
        check.setHeadline(guide.headline());
        check.setImmediateActions(guide.immediateActions());
        check.setAvoidActions(guide.avoidActions());
        check.setObservationChecklist(guide.observationChecklist());
        check.setEscalationCriteria(guide.escalationCriteria());
        check.setOptionalQuestions(guide.optionalQuestions());
        check.setEvidenceSummary(guide.evidenceSummary());
        check.setDisclaimer(guide.disclaimer());
        check.setAvailableActions(actions);
        check.setFallbackUsed(guide.fallbackUsed());
    }

    private String guardianSummary(String riskLevel, EmergencyCheckRequest request, StructuredSymptomInput structured,
                                   EmergencyGuideResponse guide, List<MatchedEmergencyRule> matches, FoodSafety food) {
        if ("정보부족".equals(riskLevel)) {
            return EmergencyGuideFactory.INSUFFICIENT_INFO_GUIDANCE;
        }
        List<String> evidencePoints = evidencePoints(guide.evidenceSummary(), 3).stream()
                .map(TextSafety::politeGuardianTone)
                .toList();
        String action = politeSentence(firstNonBlank(guide.immediateActions()));
        String avoid = politeSentence(firstNonBlank(guide.avoidActions()));
        StringBuilder summary = new StringBuilder();
        summary.append("%s 단계입니다.\n".formatted(riskLevel));
        if (!evidencePoints.isEmpty()) {
            summary.append("확인된 근거로는 ").append(String.join(" ", evidencePoints)).append("\n");
        } else if (food != null) {
            summary.append(food.getFoodName()).append(" 관련 안전 정보를 근거로 참고했습니다.\n");
        } else {
            summary.append(inputBasisSentence(request, structured)).append("\n");
        }
        if (action != null) summary.append("지금은 ").append(action).append(".\n");
        if (avoid != null) summary.append(avoid).append(".");
        String fallbackSummary = TextSafety.clean(TextSafety.politeGuardianTone(summary.toString()), 720);
        return aiClient.rewriteGuardianSummary(fallbackSummary, riskLevel, request, guide, food)
                .orElse(fallbackSummary);
    }

    private String inputBasisSentence(EmergencyCheckRequest request, StructuredSymptomInput structured) {
        List<String> points = new ArrayList<>();
        if (request.occurredTimeText() != null && !request.occurredTimeText().isBlank()) {
            points.add("발생 시점 " + TextSafety.clean(request.occurredTimeText(), 80));
        }
        if (request.repeatCount() != null) {
            points.add("반복 " + request.repeatCount() + "회");
        }
        List<String> tags = request.symptomTags() == null ? List.of() : request.symptomTags();
        if (!tags.isEmpty()) {
            points.add("선택 증상 " + String.join(", ", tags));
        }
        List<String> redFlags = request.redFlags() == null ? List.of() : request.redFlags();
        points.add(redFlags.isEmpty() ? "빠른 위험 신호 없음" : "빠른 위험 신호 " + String.join(", ", redFlags));
        if (request.suspectedFoodText() != null && !request.suspectedFoodText().isBlank()) {
            points.add("의심 물질 " + TextSafety.clean(request.suspectedFoodText(), 120));
        } else {
            points.add("의심 음식/물질 미확인");
        }
        List<String> keywords = structured == null || structured.symptomKeywords() == null ? List.of() : structured.symptomKeywords();
        if (!keywords.isEmpty()) {
            points.add("AI 추출 증상 " + String.join(", ", keywords));
        }
        return "직접 매칭되는 음식/독성 근거는 확인되지 않았지만, "
                + String.join(", ", points)
                + " 입력을 근거로 보수적으로 판단했습니다.";
    }

    private String politeSentence(String value) {
        if (value == null) return null;
        String sentence = TextSafety.politeGuardianTone(trimPeriod(value));
        return sentence == null || sentence.isBlank() ? null : sentence;
    }

    private String vetRiskSummary(EmergencyCheck check, EmergencyCheckRequest request, StructuredSymptomInput structured,
                                  EmergencyGuideResponse guide, List<MatchedEmergencyRule> matches, FoodSafety food) {
        List<String> tags = request.symptomTags() == null ? List.of() : request.symptomTags();
        List<String> observed = structured == null || structured.observedSigns() == null ? List.of() : structured.observedSigns();
        List<String> keywords = structured == null || structured.symptomKeywords() == null ? List.of() : structured.symptomKeywords();
        StringBuilder summary = new StringBuilder();
        summary.append("보호자 입력 증상: ").append(TextSafety.clean(request.symptomNote(), 600)).append("\n");
        if (request.occurredTimeText() != null && !request.occurredTimeText().isBlank()) {
            summary.append("발생 시점: ").append(TextSafety.clean(request.occurredTimeText(), 80)).append("\n");
        }
        summary.append("응급도 분류: ").append(check.getRiskLevel()).append("\n");
        summary.append("권장 방향: ").append(check.getImmediateVet() ? "즉시 병원 방문이 권장됩니다." : "상태 변화를 관찰하고 악화되면 병원 문의가 권장됩니다.").append("\n");
        if (!tags.isEmpty()) summary.append("선택 태그: ").append(String.join(", ", tags)).append("\n");
        if (request.redFlags() != null && !request.redFlags().isEmpty()) summary.append("빠른 위험 신호: ").append(String.join(", ", request.redFlags())).append("\n");
        if (!keywords.isEmpty()) summary.append("구조화된 증상 키워드: ").append(String.join(", ", keywords)).append("\n");
        if (!observed.isEmpty()) summary.append("관찰 신호: ").append(String.join(", ", observed)).append("\n");
        if (request.repeatCount() != null) summary.append("반복 횟수: ").append(request.repeatCount()).append("회\n");
        if (request.currentWeight() != null) summary.append("현재 체중: ").append(request.currentWeight()).append("kg\n");
        if (food != null) summary.append("관련 음식/물질 정보: ").append(food.getFoodName()).append(" / ").append(food.getRiskLevel()).append("\n");
        if (request.exposureAmount() != null && !request.exposureAmount().isBlank()) {
            summary.append("섭취량/노출량: ").append(TextSafety.clean(request.exposureAmount(), 160)).append("\n");
        }
        return TextSafety.clean(summary.toString(), 1200);
    }

    private List<String> evidencePoints(List<EvidenceSummary> evidence, int limit) {
        if (evidence == null || evidence.isEmpty()) return List.of();
        return evidence.stream()
                .map(EvidenceSummary::summary)
                .filter(value -> value != null && !value.isBlank())
                .filter(this::isUserFacingEvidence)
                .map(value -> trimPeriod(TextSafety.clean(value, 180)) + ".")
                .distinct()
                .limit(limit)
                .toList();
    }

    private boolean isUserFacingEvidence(String value) {
        return value != null
                && !value.contains("응급 안내 결과에는 진단 대체 불가 문구")
                && !value.contains("병원 연락/방문 권장 문구를 항상 포함");
    }

    private String firstNonBlank(List<String> values) {
        if (values == null) return null;
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String trimPeriod(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[.。]+$", "");
    }

    private String listHtml(String jsonList, String fallback) {
        List<String> values = readStringList(jsonList);
        if (values.isEmpty() && fallback != null && !fallback.isBlank()) values = List.of(fallback);
        if (values.isEmpty()) return "<p>기록된 항목이 없습니다.</p>";
        return "<ul>" + values.stream()
                .map(value -> "<li>" + escape(value) + "</li>")
                .reduce("", String::concat) + "</ul>";
    }

    private String recommendedActionsSectionHtml(EmergencyCheck check, VetReport report) {
        List<String> values = readStringList(report.getImmediateActions());
        if (values.isEmpty() && check.getRecommendedAction() != null && !check.getRecommendedAction().isBlank()) {
            values = List.of(check.getRecommendedAction());
        }
        String summaryRecommendation = riskSummaryValue(pdfRiskSummary(check, report), "권장 방향");
        List<String> filtered = values.stream()
                .filter(value -> !sameActionText(value, summaryRecommendation))
                .toList();
        if (filtered.isEmpty()) return "";
        String items = filtered.stream()
                .map(value -> "<li>" + escape(value) + "</li>")
                .reduce("", String::concat);
        return "<div class=\"box action-box\"><h2>권장 행동</h2><ul>" + items + "</ul></div>";
    }

    private String riskSummaryValue(String summary, String label) {
        if (summary == null || summary.isBlank() || label == null || label.isBlank()) return "";
        String prefix = label + ":";
        for (String line : summary.split("\\R")) {
            String value = line == null ? "" : line.trim();
            if (value.startsWith(prefix)) {
                return value.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    private boolean sameActionText(String left, String right) {
        String normalizedLeft = normalizeActionText(left);
        String normalizedRight = normalizeActionText(right);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeActionText(String value) {
        if (value == null) return "";
        return TextSafety.clean(value, 300)
                .toLowerCase()
                .replaceAll("[\\s\\p{Punct}。．]+", "");
    }

    private String evidenceSourceHtml(String json) {
        List<EvidenceSummary> values = readEvidenceSummaries(json);
        if (values.isEmpty()) return "";
        String items = values.stream()
                .limit(5)
                .map(item -> {
                    String source = firstNonBlankText(item.source(), "근거");
                    String title = firstNonBlankText(item.title(), "제목 없음");
                    String summary = item.summary() == null ? "" : item.summary();
                    String url = item.sourceUrl() == null ? "" : item.sourceUrl();
                    String label = escape(source + " - " + title);
                    String titleHtml = url.isBlank()
                            ? "<strong class=\"source-title\">" + label + "</strong>"
                            : "<a class=\"source-title\" href=\"" + escape(url) + "\">" + label + "</a>";
                    return "<li>" + titleHtml
                            + (summary.isBlank() ? "" : "<small>" + escape(summary) + "</small>")
                            + "</li>";
                })
                .reduce("", String::concat);
        return "<div class=\"box source-box\"><h2>참고 근거 출처</h2><ul class=\"source-list\">" + items + "</ul></div>";
    }

    private List<EvidenceSummary> readEvidenceSummaries(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readerForListOf(EvidenceSummary.class).readValue(json);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String firstNonBlankText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String riskCssClass(String riskLevel) {
        String value = riskLevel == null ? "" : riskLevel;
        if (value.contains("위험") || value.contains("응급") || value.contains("긴급")) return "urgent";
        if (value.contains("주의")) return "caution";
        if (value.contains("관찰")) return "observe";
        return "safe";
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String pdfRiskSummaryHtml(EmergencyCheck check, VetReport report) {
        String summary = pdfRiskSummary(check, report);
        if (summary == null || summary.isBlank()) {
            return "<p>기록된 위험 요약이 없습니다.</p>";
        }
        StringBuilder html = new StringBuilder("<dl class=\"summary-list\">");
        for (String line : summary.split("\\R")) {
            String value = line == null ? "" : line.trim();
            if (value.isBlank()) continue;
            int separator = value.indexOf(':');
            if (separator > 0) {
                html.append("<div class=\"summary-row\"><dt>")
                        .append(escape(value.substring(0, separator).trim()))
                        .append("</dt><dd>")
                        .append(escape(value.substring(separator + 1).trim()))
                        .append("</dd></div>");
            } else {
                html.append("<div class=\"summary-row\"><dt>요약</dt><dd>")
                        .append(escape(value))
                        .append("</dd></div>");
            }
        }
        html.append("</dl>");
        return html.toString();
    }

    private String pdfRiskSummary(EmergencyCheck check, VetReport report) {
        String current = report.getRiskSummary();
        boolean notUseful = current == null
                || current.isBlank()
                || current.contains("반려견 응급 상황 안내")
                || current.contains("참고 근거는 아래 근거 내용");
        if (!notUseful) return sanitizeRiskSummary(current);
        List<String> tags = readStringList(check.getSymptomTags());
        StringBuilder summary = new StringBuilder();
        summary.append("보호자 입력 증상: ").append(TextSafety.clean(check.getSymptomNote(), 600)).append("\n");
        summary.append("응급도 분류: ").append(check.getRiskLevel()).append("\n");
        summary.append("권장 방향: ").append(check.getImmediateVet() ? "즉시 병원 방문이 권장됩니다." : "상태 변화를 관찰하고 악화되면 병원 문의가 권장됩니다.").append("\n");
        if (!tags.isEmpty()) summary.append("선택 태그: ").append(String.join(", ", tags)).append("\n");
        if (check.getRepeatCount() != null) summary.append("반복 횟수: ").append(check.getRepeatCount()).append("회");
        return TextSafety.clean(summary.toString(), 900);
    }

    private String photoSectionHtml(String photoUrlsJson) {
        List<String> photos = readStringList(photoUrlsJson).stream()
                .filter(this::isSafePhotoReference)
                .limit(5)
                .toList();
        if (photos.isEmpty()) return "";
        String images = photos.stream()
                .map(src -> "<img src=\"" + escape(src) + "\" alt=\"보호자 첨부 사진\"/>")
                .reduce("", String::concat);
        return "<div class=\"box photo-box\"><h2>보호자 첨부 사진</h2>"
                + "<p>사진은 AI 응급도 판단 근거로 사용되지 않았으며, 수의사에게 상황을 설명하기 위한 참고 자료입니다.</p>"
                + "<div class=\"photo-grid\">" + images + "</div></div>";
    }

    private boolean isSafePhotoReference(String value) {
        if (value == null) return false;
        String trimmed = value.trim().toLowerCase();
        return trimmed.startsWith("data:image/") || trimmed.startsWith("https://") || trimmed.startsWith("http://");
    }

    private String sanitizeRiskSummary(String value) {
        if (value == null || value.isBlank()) return value;
        return TextSafety.clean(value, 1200)
                .replace("응급 안내 결과에는 진단 대체 불가 문구와 병원 연락/방문 권장 문구를 항상 포함합니다.", "")
                .replaceAll("응급도 분류: ([^\\n/]+) / ([^\\n]+)", "응급도 분류: $1\n권장 방향: $2")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String petSnapshot(Pet pet) {
        try {
            if (pet == null) {
                return objectMapper.writeValueAsString(java.util.Map.of("selected", false));
            }
            java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("selected", true);
            snapshot.put("name", pet.getName());
            snapshot.put("breed", pet.getBreed());
            snapshot.put("age", pet.getAge());
            snapshot.put("weight", pet.getWeight());
            snapshot.put("gender", pet.getGender());
            snapshot.put("neutered", pet.getNeutered());
            snapshot.put("allergies", pet.getAllergies());
            snapshot.put("diseases", pet.getDiseases());
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            return "{\"selected\":false}";
        }
    }

    private String petReportText(String petSnapshot) {
        if (petSnapshot == null || petSnapshot.isBlank()) return "반려견 정보가 기록되지 않았습니다.";
        try {
            Map<?, ?> pet = objectMapper.readValue(petSnapshot, Map.class);
            if (!Boolean.TRUE.equals(pet.get("selected"))) return "반려견 정보가 기록되지 않았습니다.";
            List<String> lines = new ArrayList<>();
            addPetLine(lines, "이름", pet.get("name"));
            addPetLine(lines, "견종", pet.get("breed"));
            addPetLine(lines, "나이", pet.get("age") == null ? null : pet.get("age") + "살");
            addPetLine(lines, "등록 체중", pet.get("weight") == null ? null : pet.get("weight") + "kg");
            addPetLine(lines, "성별", genderLabel(pet.get("gender")));
            addPetLine(lines, "중성화 여부", neuteredLabel(pet.get("neutered")));
            addPetLine(lines, "알레르기", pet.get("allergies"));
            addPetLine(lines, "기저질환", pet.get("diseases"));
            return lines.isEmpty() ? "반려견 정보가 기록되지 않았습니다." : String.join("\n", lines);
        } catch (Exception ex) {
            return "반려견 정보가 기록되지 않았습니다.";
        }
    }

    private void addPetLine(List<String> lines, String label, Object value) {
        if (value == null || String.valueOf(value).isBlank()) return;
        lines.add(label + ": " + value);
    }

    private String genderLabel(Object value) {
        if (value == null) return null;
        return switch (String.valueOf(value)) {
            case "MALE" -> "남아";
            case "FEMALE" -> "여아";
            case "UNKNOWN" -> "선택 안 함";
            default -> String.valueOf(value);
        };
    }

    private String neuteredLabel(Object value) {
        if (value == null) return null;
        if (Boolean.TRUE.equals(value)) return "예";
        if (Boolean.FALSE.equals(value)) return "아니오";
        return String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<Path> koreanFonts() {
        return List.of(
                Path.of("C:/Windows/Fonts/malgun.ttf"),
                Path.of("C:/Windows/Fonts/malgunsl.ttf"),
                Path.of("C:/Windows/Fonts/malgunbd.ttf")
        ).stream().filter(Files::exists).limit(1).toList();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
