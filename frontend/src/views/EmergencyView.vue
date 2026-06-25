<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { emergencyApi, petApi, petPlanApi } from "../api/resources";
import AppToast from "../components/AppToast.vue";
import CustomSelect from "../components/CustomSelect.vue";
import { formatNotice } from "../utils/messageFormat";

const pets = ref([]);
const petSelectOptions = computed(() => [
  { value: "", label: "선택 안 함" },
  ...pets.value.map((p) => ({ value: p.id, label: p.name })),
]);
const result = ref(null);
const report = ref(null);
const error = ref("");
const toast = reactive({ show: false, type: "error", message: "" });
const submitting = ref(false);
const photoError = ref("");
const checkFormRef = ref(null);
const resultPanelHeight = ref(0);
const activeResultTab = ref("summary");
const selectedCarePlanId = ref("");
const recentChecks = ref([]);
const recentPlans = ref([]);
const emergencyStateKey = "mungz_emergency_check_state";
const managementWindowDays = 14;
let formResizeObserver = null;
let toastTimer = null;
const resultTabs = [
  { key: "summary", label: "위험 요약" },
  { key: "evidence", label: "위험 근거" },
  { key: "records", label: "관리 기록" },
];
const symptomOptions = ["구토", "설사", "호흡곤란", "경련", "출혈", "무기력", "절뚝거림", "기침", "고열", "이물질 삼킴", "중독 의심", "외상"];
const maxPhotoCount = 5;
const maxPhotoSize = 900 * 1024;
const emptyForm = () => ({
  petId: "",
  currentWeight: "",
  occurredTimeText: "",
  suspectedFoodText: "",
  symptomNote: "",
  symptomTags: [],
  photoUrls: [],
});
const form = reactive(emptyForm());
const selectedPet = computed(() => pets.value.find((pet) => String(pet.id) === String(form.petId)) || null);
const selectedPetName = computed(() => selectedPet.value?.name || "선택 안 함");
const requiredFieldMessages = computed(() => {
  const messages = [];
  if (!form.petId) messages.push("반려견을 선택해 주세요.");
  if (!form.symptomNote.trim()) {
    messages.push("증상 메모를 입력해 주세요.");
  } else if (form.symptomNote.trim().length < 5) {
    messages.push("증상 메모는 5자 이상 입력해 주세요. 언제부터, 몇 번, 어떤 모습인지 함께 적어 주세요.");
  }
  return messages;
});
const evidenceItems = computed(() => publicEvidenceItems(result.value?.evidenceSummary || []));
const evidenceTotalCount = computed(() => evidenceItems.value.length);
const resultPanelStyle = computed(() => resultPanelHeight.value ? { "--result-card-height": `${resultPanelHeight.value}px` } : {});
const relatedCarePlans = computed(() => {
  if (!form.petId) return [];
  const petId = String(form.petId);
  const currentId = result.value?.id;
  const currentContext = [form.symptomNote, form.symptomTags.join(" "), result.value?.riskReason].join(" ");
  const currentTokens = symptomTokens(currentContext);
  const currentCategories = symptomCategories(currentContext);
  const isRelatedText = (value) => {
    const planCategories = symptomCategories(value);
    if (!hasCategoryOverlap(currentCategories, planCategories)) return false;
    return hasMeaningfulTokenOverlap(currentTokens, symptomTokens(value));
  };
  return recentPlans.value
    .filter((plan) => String(plan.petId) === petId)
    .filter((plan) => isWithinManagementWindow(plan.planDate))
    .filter((plan) => plan.emergencyCheckId === currentId || isRelatedText(`${plan.title || ""} ${plan.memo || ""} ${plan.category || ""}`))
    .map((plan) => ({
      id: plan.id,
      emergencyCheckId: plan.emergencyCheckId,
      date: plan.planDate,
      category: plan.category,
      title: plan.title,
      memo: plan.memo,
    }))
    .sort((a, b) => String(b.date || "").localeCompare(String(a.date || "")));
});
const selectedCarePlan = computed(() => relatedCarePlans.value.find((item) => carePlanKey(item) === selectedCarePlanId.value) || null);
const selectedCareReportRoute = computed(() => {
  if (!selectedCarePlan.value) return null;
  const reportId = selectedCarePlan.value?.emergencyCheckId || result.value?.id;
  return reportId ? { path: "/reports", query: { reportId } } : null;
});
const careManagementDays = computed(() => {
  const dates = relatedCarePlans.value
    .map((item) => parseDate(item.date))
    .filter(Boolean);
  const currentDate = result.value?.createdAt ? parseDate(result.value.createdAt) : new Date();
  dates.push(currentDate || new Date());
  const first = dates.sort((a, b) => a.getTime() - b.getTime())[0];
  if (!first) return 1;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  first.setHours(0, 0, 0, 0);
  return Math.max(1, Math.floor((today.getTime() - first.getTime()) / 86400000) + 1);
});
const videoKeywordByCategory = {
  "CPR/심폐소생술": "CPR",
  "응급상황 대처": "응급",
  "호흡기 증상": "호흡",
  "구토/소화기 증상": "구토",
  "설사/소화기 증상": "설사",
  "구토/설사/소화기 증상": "구토",
  "발작/경련": "경련",
  "기도폐쇄/하임리히": "하임리히",
  "이물섭취/응급처치": "이물",
  "이물섭취/위험물질": "이물",
  "음식주의/중독": "중독",
  "위험신호/건강체크": "위험신호",
};
function relatedVideoKeyword(category) {
  if (videoKeywordByCategory[category]) return videoKeywordByCategory[category];
  return form.symptomTags.find((tag) => symptomOptions.includes(tag)) || "";
}
const relatedVideoRoute = computed(() => {
  const query = {};
  const category = result.value?.availableActions?.videoCategoryCode;
  const keyword = relatedVideoKeyword(category);
  if (category) query.category = category;
  if (keyword) query.keyword = keyword;
  return { path: "/videos", query };
});
const showRelatedVideoButton = computed(() => !!result.value && !isInsufficientInfo(result.value.riskLevel));
const careRecordRoute = computed(() => {
  const today = new Date();
  const planDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  const memo = [
    `응급 체크 결과: ${result.value?.riskLevel || "-"}`,
    `입력 증상: ${form.symptomNote || "-"}`,
    form.symptomTags.length ? `증상 태그: ${form.symptomTags.join(", ")}` : "",
    result.value?.recommendedAction ? `권장 조치: ${result.value.recommendedAction}` : "",
  ].filter(Boolean).join("\n");
  return {
    path: "/records",
    query: {
      tab: "plans",
      petId: form.petId,
      emergencyCheckId: result.value?.id || "",
      category: "응급 체크",
      title: `${selectedPetName.value} 응급 체크 기록`,
      planDate,
      memo,
    },
  };
});
async function load() {
  try {
    pets.value = await petApi.list();
    if (form.petId && form.currentWeight === "" && selectedPet.value?.weight != null) {
      form.currentWeight = selectedPet.value.weight;
    }
  } catch (e) {
    pets.value = [];
    error.value = "반려견 목록을 불러오지 못했어요. 응급 체크를 진행하려면 등록된 반려견을 선택해야 합니다.";
  }
  try {
    [recentChecks.value, recentPlans.value] = await Promise.all([emergencyApi.list(), petPlanApi.list()]);
  } catch (e) {
    recentChecks.value = [];
    recentPlans.value = [];
  }
}

function toggleSymptom(tag) {
  form.symptomTags = form.symptomTags.includes(tag) ? form.symptomTags.filter((v) => v !== tag) : [...form.symptomTags, tag];
}

function handlePhotoUpload(event) {
  photoError.value = "";
  const files = Array.from(event.target.files || []);
  event.target.value = "";
  const remaining = maxPhotoCount - form.photoUrls.length;
  if (remaining <= 0) {
    photoError.value = "사진은 최대 5장까지 첨부할 수 있습니다.";
    return;
  }
  files.slice(0, remaining).forEach((file) => {
    if (!file.type.startsWith("image/")) {
      photoError.value = "이미지 파일만 첨부할 수 있습니다.";
      return;
    }
    if (file.size > maxPhotoSize) {
      photoError.value = "사진은 장당 900KB 이하로 첨부해 주세요.";
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === "string" && form.photoUrls.length < maxPhotoCount) {
        form.photoUrls = [...form.photoUrls, reader.result];
      }
    };
    reader.readAsDataURL(file);
  });
}

function removePhoto(index) {
  form.photoUrls = form.photoUrls.filter((_, i) => i !== index);
}

function resetConsult() {
  Object.assign(form, emptyForm());
  result.value = null;
  report.value = null;
  selectedCarePlanId.value = "";
  activeResultTab.value = "summary";
  error.value = "";
  photoError.value = "";
  sessionStorage.removeItem(emergencyStateKey);
}

function saveEmergencyState() {
  if (!result.value) return;
  sessionStorage.setItem(emergencyStateKey, JSON.stringify({
    form: { ...form },
    result: result.value,
    report: report.value,
  }));
}

function markEmergencyReturn() {
  saveEmergencyState();
  window.history.replaceState({ ...window.history.state, emergencyRestore: true }, "");
}

function showToast(message, type = "error") {
  const notice = formatNotice(message);
  error.value = notice;
  toast.message = notice;
  toast.type = type;
  toast.show = true;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.show = false;
  }, 3200);
}

async function downloadVetReportPdf(id) {
  if (!id) return;
  try {
    await emergencyApi.downloadReportPdf(id);
  } catch (e) {
    showToast(e?.message || "PDF 다운로드를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.", "error");
  }
}

function carePlanKey(item) {
  return String(item?.id || `${item?.date || ""}-${item?.title || ""}-${item?.emergencyCheckId || ""}`);
}

async function updateResultPanelHeight() {
  await nextTick();
  const formHeight = checkFormRef.value?.getBoundingClientRect?.().height || 0;
  resultPanelHeight.value = formHeight ? Math.round(formHeight) : 0;
}

function restoreEmergencyState() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(emergencyStateKey) || "null");
    if (!saved?.result) return false;
    Object.assign(form, emptyForm(), saved.form || {});
    result.value = saved.result;
    report.value = saved.report || null;
    activeResultTab.value = "summary";
    return true;
  } catch {
    sessionStorage.removeItem(emergencyStateKey);
    return false;
  }
}

function requiredFieldNotice() {
  const fields = [];
  if (!form.petId) fields.push("반려견 선택");
  if (!form.symptomNote.trim()) {
    fields.push("증상 메모 입력");
  } else if (form.symptomNote.trim().length < 5) {
    fields.push("증상 메모 5자 이상 입력");
  }
  return `${fields.join(", ")}을 완료한 뒤 다시 시도해 주세요.`;
}

function shouldHideRiskSummaryLine(line = "") {
  return /상위 독소|우선 매칭|매칭 대상|룰과 연결|룰로 분류|응급 룰|위험 룰|룰 설계|근거 판단 룰|구조화된 증상 키워드|근거 도출|내부 로직|food_safety|emergency_rules/i.test(line);
}

function publicEvidenceItems(items = []) {
  return items
    .map((item) => ({
      ...item,
      summary: shouldHideRiskSummaryLine(item.summary || "") ? "" : item.summary,
      recommendedAction: shouldHideRiskSummaryLine(item.recommendedAction || "") ? "" : item.recommendedAction,
      avoidAction: shouldHideRiskSummaryLine(item.avoidAction || "") ? "" : item.avoidAction,
    }))
    .filter((item) => item.summary || item.recommendedAction || item.avoidAction);
}

function riskSummaryParagraphs(value = "") {
  const raw = String(value || "")
    .replace(/\s*(위험|주의|관찰|정보부족)\s*단계입니다\.\s*/g, "\n$1 단계입니다.\n")
    .replace(/\s*(반려견[^.?!。]*경우,?)\s*/g, "\n$1 ")
    .replace(/\s*(초콜릿|코코아|카카오|포도|자일리톨|양파|마늘)([^.?!。]*(?:초래|유발|위험|독성)[^.?!。]*[.?!。])\s*/g, "\n$1$2\n")
    .replace(/\s*((?:가능하면|섭취한|섭취량|제품|포장지|라벨|증상|이동)[^.?!。]*(?:준비|확인|연락|이동|문의)[^.?!。]*[.?!。])\s*/g, "\n$1\n")
    .replace(/([.!?。])\s+(?=(?:반려견|초콜릿|코코아|카카오|초코|가능하면|섭취|제품|포장지|증상|지금|병원|본 안내))/g, "$1\n");
  return raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !shouldHideRiskSummaryLine(line))
    .map((line) => line.replace(/^(요약|응급도|판단 근거|지금 할 일|피해야 할 일)\s*:\s*/, ""))
    .filter(Boolean);
}

function isInsufficientInfo(level) {
  return /정보부족|정보 부족|insufficient|unknown/i.test(String(level || ""));
}

function symptomTokens(value = "") {
  const normalized = String(value || "")
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, " ");
  const stopTokens = new Set(["응급", "체크", "결과", "입력", "증상", "권장", "조치", "기록", "관리", "주의", "위험", "관찰", "병원", "문의", "이동", "오늘"]);
  const tokens = normalized.split(/\s+/).filter((token) => token.length >= 2 && !stopTokens.has(token));
  const aliases = [];
  if (/절|쩔뚝|다리|삐끗|골절|부러/.test(value)) aliases.push("절뚝거림");
  if (/구토|토/.test(value)) aliases.push("구토");
  if (/설사|변/.test(value)) aliases.push("설사");
  if (/눈|충혈|비벼|빨갛|빨게/.test(value)) aliases.push("눈");
  if (/초콜릿|초코|코코아|카카오|포도|중독|이물|삼킴/.test(value)) aliases.push("중독");
  return new Set([...tokens, ...aliases]);
}

function symptomCategories(value = "") {
  const text = String(value || "");
  const categories = new Set();
  if (/절|쩔뚝|다리|삐끗|골절|부러|발톱|외상|상처|출혈|멍|부종|통증/.test(text)) categories.add("injury");
  if (/초콜릿|초코|코코아|카카오|포도|건포도|자일리톨|양파|마늘|약|중독|독성|이물|삼킴|먹|핥/.test(text)) categories.add("toxin");
  if (/구토|토했|설사|혈변|검은변|복통/.test(text)) categories.add("digestive");
  if (/눈|충혈|비벼|빨갛|각막|안과/.test(text)) categories.add("eye");
  if (/호흡|숨|기침|헐떡|잇몸|혀/.test(text)) categories.add("breathing");
  if (/경련|발작|비틀|실신|의식|쓰러/.test(text)) categories.add("neuro");
  return categories;
}

function hasCategoryOverlap(a, b) {
  if (!a?.size || !b?.size) return false;
  return [...a].some((token) => b.has(token));
}

function hasMeaningfulTokenOverlap(a, b) {
  if (!a?.size || !b?.size) return false;
  return [...a].filter((token) => token.length >= 2).some((token) => b.has(token));
}

function parseDate(value) {
  if (!value) return null;
  const date = new Date(String(value).replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function isWithinManagementWindow(value) {
  const date = parseDate(value);
  if (!date) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  date.setHours(0, 0, 0, 0);
  const diff = Math.floor((today.getTime() - date.getTime()) / 86400000);
  return diff >= 0 && diff <= managementWindowDays;
}

function riskClass(level) {
  const value = String(level || "");
  if (isInsufficientInfo(value)) return "unknown";
  if (/위험|응급|긴급|danger|high/i.test(value)) return "urgent";
  if (/주의|중간|caution|medium/i.test(value)) return "caution";
  if (/관찰|observe|watch|low/i.test(value)) return "observe";
  return "safe";
}

async function submit() {
  error.value = "";
  report.value = null;
  selectedCarePlanId.value = "";
  if (requiredFieldMessages.value.length > 0) {
    showToast(requiredFieldNotice(), "error");
    return;
  }
  submitting.value = true;
  try {
    result.value = await emergencyApi.create({
      petId: Number(form.petId),
      currentWeight: form.currentWeight === "" ? null : Number(form.currentWeight),
      occurredTimeText: form.occurredTimeText || null,
      repeatCount: 1,
      suspectedFoodText: form.suspectedFoodText || null,
      exposureAmount: null,
      symptomNote: form.symptomNote,
      symptomTags: form.symptomTags,
      photoUrls: form.photoUrls,
    });
    if (result.value?.id) {
      report.value = await emergencyApi.report(result.value.id);
    }
    activeResultTab.value = "summary";
    updateResultPanelHeight();
    saveEmergencyState();
  } catch (e) {
    showToast(e?.message || "응급 체크를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.", "error");
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  const shouldRestore = Boolean(window.history.state?.emergencyRestore);
  if (!shouldRestore) {
    sessionStorage.removeItem(emergencyStateKey);
  }
  if (!restoreEmergencyState()) {
    Object.assign(form, emptyForm());
    result.value = null;
    report.value = null;
    activeResultTab.value = "summary";
    error.value = "";
    photoError.value = "";
  }
  load();
  updateResultPanelHeight();
  if (window.ResizeObserver && checkFormRef.value) {
    formResizeObserver = new ResizeObserver(updateResultPanelHeight);
    formResizeObserver.observe(checkFormRef.value);
  }
  window.addEventListener("resize", updateResultPanelHeight);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", updateResultPanelHeight);
  formResizeObserver?.disconnect();
  clearTimeout(toastTimer);
});

watch(() => form.petId, () => {
  if (selectedPet.value?.weight != null) {
    form.currentWeight = selectedPet.value.weight;
  } else if (!form.petId) {
    form.currentWeight = "";
  }
});

watch(activeResultTab, updateResultPanelHeight);

watch(relatedCarePlans, (plans) => {
  if (!plans.some((item) => carePlanKey(item) === selectedCarePlanId.value)) {
    selectedCarePlanId.value = "";
  }
});
</script>

<template>
  <main class="workspace check-layout">
    <AppToast
      :show="toast.show"
      :type="toast.type"
      :message="toast.message"
      @close="toast.show = false"
    />
    <section class="diary-hero">
      <div>
        <span class="eyebrow">Emergency check</span>
        <h1>응급 체크</h1>
        <p>호흡, 경련, 외상, 소화기 증상, 음식·물질 섭취 상황을 입력하면 응급도와 다음 행동을 한눈에 확인할 수 있게 정리합니다.</p>
      </div>
    </section>
    <p v-if="error" class="message error">{{ formatNotice(error) }}</p>
    <section class="check-grid">
      <form ref="checkFormRef" class="check-panel check-form" novalidate @submit.prevent="submit">
        <label><span class="required-label">반려견</span><CustomSelect v-model="form.petId" :options="petSelectOptions" aria-label="반려견" /></label>
        <label>체중<input v-model="form.currentWeight" type="number" min="0.1" max="120" step="0.1" placeholder="kg" /></label>
        <label>발생 시점<input v-model="form.occurredTimeText" placeholder="예: 방금, 2시간 전, 어제부터" /></label>
        <label>의심 음식/물질<input v-model="form.suspectedFoodText" placeholder="선택 사항: 초콜릿, 포도, 약품 등" /></label>
        <label class="wide"><span class="required-label">증상 메모</span><textarea v-model="form.symptomNote" rows="5" required placeholder="예: 산책 후 갑자기 헐떡이고 몸을 떨며 일어나지 못합니다." /></label>
        <div class="wide emergency-photo-field symptom-tag-field">
          <span class="label-title">증상 태그 <span class="optional-badge">선택</span></span>
          <div class="symptom-chip-grid">
            <button v-for="tag in symptomOptions" :key="tag" type="button" class="symptom-chip" :class="{ selected: form.symptomTags.includes(tag) }" @click="toggleSymptom(tag)">{{ tag }}</button>
          </div>
        </div>
        <div class="wide emergency-photo-field">
          <div class="photo-field-head">
            <span class="label-title">사진 첨부</span>
            <span>{{ form.photoUrls.length }} / {{ maxPhotoCount }}</span>
          </div>
          <div class="emergency-photo-grid">
            <div v-for="(src, index) in form.photoUrls" :key="`${src.slice(0, 32)}-${index}`" class="emergency-photo-thumb">
              <img :src="src" alt="첨부 사진 미리보기" />
              <button type="button" aria-label="사진 삭제" @click="removePhoto(index)">×</button>
            </div>
            <label v-if="form.photoUrls.length < maxPhotoCount" class="emergency-photo-add">
              <input type="file" accept="image/*" multiple @change="handlePhotoUpload" />
              <span>+</span>
              <strong>사진 추가</strong>
            </label>
          </div>
          <small>
            <span>사진은 선택 사항이며 최대 5장까지 첨부할 수 있습니다.</span>
            <span>수의사에게 상황을 설명하는 참고 자료로 함께 전달됩니다.</span>
          </small>
          <small v-if="photoError" class="photo-error">{{ photoError }}</small>
        </div>
        <div class="ai-context-card">
          <strong>응급도 확인 기준</strong>
          <span>입력한 증상과 반려견 정보를 함께 확인해 위험도를 분류합니다.</span>
          <small>결과는 진단이 아닌 보호자용 안내입니다. 위험 신호가 있으면 즉시 병원 방문을 우선해 주세요.</small>
        </div>
        <button class="primary" type="submit" :disabled="submitting">{{ submitting ? "확인 중..." : "응급 체크하기" }}</button>
      </form>
      <section class="check-result" :style="resultPanelStyle" :class="{ 'is-empty': !submitting && !result, 'is-loading': submitting }">
        <div v-if="submitting" class="recovery-panel loading">
          <strong>AI 응급 체크 결과를 생성하고 있습니다.</strong>
          <p>입력 내용을 정리하고 응급도, 권장 행동, PDF 리포트 흐름을 준비하는 중입니다.</p>
        </div>
        <template v-else-if="result">
          <div class="check-result-header">
            <div class="result-title-group">
              <span class="risk-level" :class="riskClass(result.riskLevel)">{{ result.riskLevel }}</span>
              <h2>응급 체크 결과</h2>
            </div>
            <button v-if="result.id" type="button" class="result-pdf-link" @click="downloadVetReportPdf(result.id)">
              수의사 리포트 PDF
            </button>
          </div>
          <div class="check-result-body">
            <div class="result-tab-shell">
              <div class="result-tab-list" role="tablist" aria-label="응급 체크 결과 분류">
                <button
                  v-for="tab in resultTabs"
                  :id="`result-tab-${tab.key}`"
                  :key="tab.key"
                  type="button"
                  class="result-tab-button"
                  role="tab"
                  :aria-selected="activeResultTab === tab.key"
                  :aria-controls="`result-panel-${tab.key}`"
                  :class="{ active: activeResultTab === tab.key }"
                  @click="activeResultTab = tab.key"
                >
                  {{ tab.label }}
                </button>
              </div>
              <section
                v-if="activeResultTab === 'summary'"
                id="result-panel-summary"
                class="result-tab-panel"
                role="tabpanel"
                aria-labelledby="result-tab-summary"
              >
              <section class="result-input-summary emergency-caution-card">
                <strong>응급 체크 안내 및 주의사항</strong>
                <p>AI 응급 체크 결과는 보호자 판단을 돕는 참고 자료입니다.<br />호흡곤란, 경련, 지속 출혈, 의식 저하가 있으면 결과와 관계없이 즉시 병원으로 이동하세요.</p>
              </section>
              <section v-if="riskSummaryParagraphs(result.analysisResult).length" class="result-input-summary ai-summary-card risk-summary-toggle-card">
                <strong>위험 요약</strong>
                <div class="risk-summary-paragraphs always-open">
                  <p v-for="line in riskSummaryParagraphs(result.analysisResult)" :key="line">{{ line }}</p>
                </div>
              </section>
              <section v-if="!isInsufficientInfo(result.riskLevel) && result.immediateActions?.length" class="result-plain-section action-list-section">
                <strong>즉시 할 일</strong>
                <ul>
                  <li v-for="item in result.immediateActions" :key="item">{{ item }}</li>
                </ul>
              </section>
              <section v-if="!isInsufficientInfo(result.riskLevel) && result.avoidActions?.length" class="result-plain-section action-list-section avoid-action-card">
                <strong>피해야 할 일</strong>
                <ul>
                  <li v-for="item in result.avoidActions" :key="item">{{ item }}</li>
                </ul>
              </section>
              <section v-if="isInsufficientInfo(result.riskLevel) && result.optionalQuestions?.length" class="result-followup-questions">
                <strong>추가로 입력해 주세요</strong>
                <p>아래 질문에 대한 답을 증상 메모에 덧붙인 뒤 다시 확인해 주세요.</p>
                <ul>
                  <li v-for="item in result.optionalQuestions" :key="item">{{ item }}</li>
                </ul>
              </section>
              <div class="result-actions">
                <RouterLink
                  v-if="showRelatedVideoButton"
                  class="primary-link"
                  :to="relatedVideoRoute"
                  @click="markEmergencyReturn"
                >
                  관련 응급처치 영상 보기
                </RouterLink>
                <RouterLink v-if="result.availableActions?.showHospitalButton" class="primary-link" to="/hospitals" @click="markEmergencyReturn">
                  근처 병원 지도에서 확인하기
                </RouterLink>
              </div>
              </section>
              <section
                v-if="activeResultTab === 'evidence'"
                id="result-panel-evidence"
                class="result-tab-panel"
                role="tabpanel"
                aria-labelledby="result-tab-evidence"
              >
              <section v-if="evidenceTotalCount" class="result-plain-section evidence-summary-card">
                <strong>근거 내용</strong>
                <div class="evidence-summary-scroll">
                  <ul>
                    <li
                      v-for="(item, index) in evidenceItems"
                      :key="`${item.ruleId}-${item.evidenceId}`"
                      class="evidence-summary-item"
                      :class="{ clickable: item.sourceUrl }"
                    >
                      <component
                        :is="item.sourceUrl ? 'a' : 'div'"
                        class="evidence-summary-link"
                        :href="item.sourceUrl || undefined"
                        :target="item.sourceUrl ? '_blank' : undefined"
                        :rel="item.sourceUrl ? 'noopener noreferrer' : undefined"
                      >
                        <b><span class="evidence-number">{{ index + 1 }}</span>{{ item.source }} - {{ item.title }}</b>
                        <span v-if="item.summary" class="evidence-row evidence-row-summary"><em>근거</em>{{ item.summary }}</span>
                        <span v-if="item.recommendedAction" class="evidence-row evidence-row-action"><em>권장</em>{{ item.recommendedAction }}</span>
                        <span v-if="item.avoidAction" class="evidence-row evidence-row-caution"><em>주의</em>{{ item.avoidAction }}</span>
                      </component>
                    </li>
                  </ul>
                </div>
              </section>
                <div v-else class="recovery-panel empty result-evidence-empty">
                  <strong>{{ isInsufficientInfo(result.riskLevel) ? "추가 정보가 필요해요" : "연결된 근거가 없습니다" }}</strong>
                  <p>
                    {{
                      isInsufficientInfo(result.riskLevel)
                        ? "위험 근거를 보여주기에는 입력된 증상 정보가 부족합니다. 위험 요약 탭의 추가 질문을 확인한 뒤 다시 응급 체크를 진행해 주세요."
                        : "현재 결과에 연결된 공개 근거 내용이 없습니다. 증상 메모를 조금 더 구체적으로 입력하면 더 정확한 근거를 확인할 수 있어요."
                    }}
                  </p>
                  <div v-if="isInsufficientInfo(result.riskLevel)">
                    <button type="button" @click="activeResultTab = 'summary'">추가 질문 확인</button>
                  </div>
                </div>
              </section>
              <section
                v-if="activeResultTab === 'records'"
                id="result-panel-records"
                class="result-tab-panel"
                role="tabpanel"
                aria-labelledby="result-tab-records"
              >
                <section class="care-record-intro-card result-plain-section">
                  <div class="plan-nudge-head">
                    <div>
                      <strong>관리 기록</strong>
                      <p>최근 {{ managementWindowDays }}일 안에 같은 반려견과 현재 증상에 연결된 관리 기록만 보여줍니다.</p>
                    </div>
                    <RouterLink
                      v-if="relatedCarePlans.length >= 2"
                      class="care-more-link"
                      :to="{ path: '/records' }"
                      @click="markEmergencyReturn"
                    >
                      더보기
                    </RouterLink>
                  </div>
                </section>
                <section class="care-record-detail-card result-plain-section">
                  <div class="care-progress-line">
                    <b>{{ careManagementDays }}일째 관리 중</b>
                    <span v-if="relatedCarePlans.length">이전 관리 기록을 참고해<br />오늘 상태 변화를 이어서 남길 수 있습니다.</span>
                    <span v-else>오늘 결과를 첫 관리 기록으로 남길 수 있습니다.</span>
                  </div>
                  <ul v-if="relatedCarePlans.length" class="care-timeline-list">
                    <li
                      v-for="(item, index) in relatedCarePlans"
                      :key="`${item.id || item.date}-${index}`"
                      class="care-timeline-item"
                      :class="{ selected: selectedCarePlanId === carePlanKey(item), expanded: selectedCarePlanId === carePlanKey(item) }"
                    >
                      <button
                        type="button"
                        class="care-timeline-select"
                        @click="selectedCarePlanId = selectedCarePlanId === carePlanKey(item) ? '' : carePlanKey(item)"
                      >
                        <span class="care-timeline-date">{{ item.date }}</span>
                        <span class="care-timeline-body">
                          <span class="care-timeline-tag">{{ item.category }}</span>
                          <p>{{ item.title }}</p>
                          <small v-if="item.memo" class="care-timeline-memo">{{ item.memo }}</small>
                        </span>
                      </button>
                    </li>
                  </ul>
                  <p v-else class="care-timeline-empty">현재 증상과 직접 연결된 이전 기록은 아직 없습니다.</p>
                  <div class="plan-nudge-actions">
                    <RouterLink v-if="result.id" class="primary-link compact" :to="careRecordRoute" @click="markEmergencyReturn">기록하기</RouterLink>
                    <RouterLink
                      v-if="selectedCareReportRoute"
                      class="primary-link compact secondary"
                      :to="selectedCareReportRoute"
                      @click="markEmergencyReturn"
                    >
                      해당 리포트 확인
                    </RouterLink>
                    <button v-else type="button" class="primary-link compact secondary disabled" disabled>해당 리포트 확인</button>
                  </div>
                </section>
              </section>
            </div>
          </div>
        </template>
        <div v-else class="check-result-placeholder">
          <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M24 4 L40 11 V24 C40 34 24 44 24 44 S8 34 8 24 V11 Z"/><path d="M18 24 l4 4 8-8"/></svg>
          <p>증상을 입력하고 <strong>응급 체크하기</strong>를 누르면<br />응급도 안내와 수의사 전달용 리포트 PDF 다운로드가 표시됩니다.</p>
        </div>
      </section>
    </section>
  </main>
</template>
