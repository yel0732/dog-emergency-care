<script setup>
import { onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { foodApi } from "../api/resources";
import { formatNotice } from "../utils/messageFormat";

const route = useRoute();
const router = useRouter();
const keyword = ref("");
const searched = ref(false);
const foods = ref([]);
const error = ref("");
const loading = ref(false);
const lastKeyword = ref("");
const recentKeywords = ref([]);
const expandedFoodIds = ref(new Set());
const selectedDetailSections = ref({});
const selectedFood = ref(null);
const quickKeywords = ["초콜릿", "포도", "양파", "자일리톨", "우유", "생감자"];
function saveRecentKeyword(value) {
  const next = [value, ...recentKeywords.value.filter((item) => item !== value)].slice(0, 5);
  recentKeywords.value = next;
  localStorage.setItem("foodRecentKeywords", JSON.stringify(next));
}

function removeRecentKeyword(value) {
  const next = recentKeywords.value.filter((item) => item !== value);
  recentKeywords.value = next;
  localStorage.setItem("foodRecentKeywords", JSON.stringify(next));
}

function clearRecentKeywords() {
  recentKeywords.value = [];
  localStorage.removeItem("foodRecentKeywords");
}

function riskClass(food) {
  return {
    danger: ["위험", "EMERGENCY", "DANGER"].includes(food.riskLevel),
    caution: ["주의", "CAUTION"].includes(food.riskLevel),
    ok: ["안전", "SAFE"].includes(food.riskLevel),
  };
}

function riskTone(food) {
  const classes = riskClass(food);
  if (classes.danger) return "danger";
  if (classes.caution) return "caution";
  if (classes.ok) return "ok";
  return "neutral";
}

function reasonSummaryLabel(food) {
  const tone = riskTone(food);
  if (tone === "danger") return "핵심 위험";
  if (tone === "caution") return "주의 포인트";
  if (tone === "ok") return "안전 포인트";
  return "핵심 정보";
}

function reasonDetailLabel(food) {
  const tone = riskTone(food);
  if (tone === "danger") return "위험 이유";
  if (tone === "caution") return "주의 이유";
  if (tone === "ok") return "안전 이유";
  return "상세 이유";
}

function tagClass(tag) {
  const value = String(tag || "");
  const hasAny = (keywords) => keywords.some((item) => value.includes(item));
  if (hasAny(["위험", "중독", "알레르기", "무기력", "발작", "과호흡", "부종", "구토", "설사", "떨림", "복통", "침", "이상"])) return "risk";
  if (hasAny(["관찰", "상담", "응급", "병원", "연락", "확인", "주의"])) return "action";
  if (hasAny(["초콜릿", "카카오", "포도", "양파", "자일리톨", "마늘", "고구마", "오이", "코코넛", "사탕", "캔디", "생육", "생선"])) return "food";
  return "symptom";
}

async function search(sample) {
  if (sample) keyword.value = sample;

  const trimmedKeyword = keyword.value.trim();
  if (!trimmedKeyword) {
    error.value = "검색어를 입력해 주세요.";
    searched.value = false;
    return;
  }

  keyword.value = trimmedKeyword;
  error.value = "";
  searched.value = true;
  loading.value = true;
  lastKeyword.value = trimmedKeyword;
  saveRecentKeyword(trimmedKeyword);
  router.replace({ query: { q: trimmedKeyword } }).catch(() => {});

  try {
    foods.value = await foodApi.list(trimmedKeyword);
    expandedFoodIds.value = new Set();
    selectedFood.value = null;
  } catch (e) {
    foods.value = [];
    error.value = "음식 안전 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
  } finally {
    loading.value = false;
  }
}

function retrySearch() {
  if (lastKeyword.value) search(lastKeyword.value);
}

function resetSearch() {
  keyword.value = "";
  lastKeyword.value = "";
  foods.value = [];
  selectedFood.value = null;
  error.value = "";
  searched.value = false;
  loading.value = false;
  router.replace({ query: {} }).catch(() => {});
}

function splitDashLines(value, fallback = "정보 없음") {
  const text = String(value || "").trim();
  if (!text) return [fallback];
  const lines = text
    .replace(/\\r\\n|\\n|\\r/g, "\n")
    .split(/(?:^|\n)\s*-\s*|\s+-\s+/)
    .map((line) => line.trim())
    .filter(Boolean);
  return lines.length ? lines : [fallback];
}

function foodKey(food) {
  return String(food.id ?? food.foodName);
}

function detailId(food) {
  return `food-detail-${foodKey(food)}`;
}

function isFoodExpanded(food) {
  return expandedFoodIds.value.has(foodKey(food));
}

function toggleFoodDetails(food) {
  const key = foodKey(food);
  const next = new Set(expandedFoodIds.value);
  if (next.has(key)) next.delete(key);
  else {
    next.add(key);
    if (!selectedDetailSections.value[key]) {
      selectedDetailSections.value = { ...selectedDetailSections.value, [key]: "dangerReason" };
    }
  }
  expandedFoodIds.value = next;
}

function openFoodDetails(food) {
  selectedFood.value = food;
}

function closeFoodDetails() {
  selectedFood.value = null;
}

function foodLines(value) {
  return splitDashLines(value, "").filter(Boolean);
}

function splitSentenceLines(value) {
  return String(value || "")
    .replace(/\r\n|\n|\r/g, "\n")
    .split("\n")
    .flatMap((line) => line
      .replace(/([.!?。！？])\s+/g, "$1\n")
      .split("\n")
    )
    .map((line) => line.trim())
    .filter(Boolean);
}

function foodSentenceLines(value, fallback = "정보 없음") {
  const lines = foodLines(value).flatMap(splitSentenceLines);
  return lines.length ? lines : [fallback];
}

function summaryLines(food) {
  const reasonLines = foodLines(food.dangerReason).slice(0, 2);
  const responseLines = foodLines(food.response).slice(0, 1);
  return [...reasonLines, ...responseLines].slice(0, 3);
}

function summaryText(food) {
  const lines = summaryLines(food);
  return lines.length ? lines.join(" / ") : "상세 정보를 확인해 주세요.";
}

function firstLine(value, fallback = "상세 정보 확인") {
  return foodLines(value)[0] || fallback;
}

function responseSectionLabel(food) {
  return riskTone(food) === "ok" ? "급여 방법" : "대처 방법";
}

function conditionSectionLabel(food) {
  return riskTone(food) === "ok" ? "안전 조건" : "위험 조건";
}

function summaryFields(food) {
  const fields = [
    { label: reasonSummaryLabel(food), value: firstLine(food.dangerReason) },
    { label: "관찰 증상", value: firstLine(food.observedSymptoms) },
    { label: responseSectionLabel(food), value: firstLine(food.response) },
  ];
  const extra = food.riskCondition || food.doseNote;
  if (extra) {
    fields.push({ label: food.riskCondition ? conditionSectionLabel(food) : "섭취량", value: firstLine(extra) });
  }
  return fields;
}

function visibleTags(food) {
  return food.tags || [];
}

function detailSections(food) {
  const sections = [
    { key: "dangerReason", label: reasonDetailLabel(food), icon: "alert", type: "lines", value: food.dangerReason },
    { key: "observedSymptoms", label: "관찰 증상", icon: "eye", type: "lines", value: food.observedSymptoms },
    { key: "response", label: responseSectionLabel(food), icon: "firstAid", type: "lines", value: food.response },
  ];
  if (food.doseNote) sections.push({ key: "doseNote", label: "섭취량", icon: "scale", type: "lines", value: food.doseNote });
  if (food.riskCondition) sections.push({ key: "riskCondition", label: conditionSectionLabel(food), icon: "condition", type: "lines", value: food.riskCondition });
  if (food.aliases?.length) sections.push({ key: "aliases", label: "다른 이름", icon: "alias", type: "text", value: food.aliases.join(", ") });
  if (food.tags?.length) sections.push({ key: "tags", label: "주요 태그", icon: "tag", type: "tags", value: visibleTags(food) });
  if (referenceItems(food).length) sections.push({ key: "references", label: "근거/참고자료", icon: "link", type: "references", value: referenceItems(food) });
  return sections;
}

function selectedDetailKey(food) {
  return selectedDetailSections.value[foodKey(food)] || "dangerReason";
}

function selectedDetailSection(food) {
  return detailSections(food).find((section) => section.key === selectedDetailKey(food)) || detailSections(food)[0];
}

function setSelectedDetailSection(food, event) {
  selectedDetailSections.value = {
    ...selectedDetailSections.value,
    [foodKey(food)]: event.target.value,
  };
}

function referenceItems(food) {
  const source = food.referenceLinks?.length ? food.referenceLinks : food.references || [];
  return source
    .map((item) => {
      if (typeof item === "string") {
        return { label: item.replace(/^[-\s]+/, "").trim(), url: "" };
      }
      return {
        label: String(item.label || item.organization || item.title || "").replace(/^[-\s]+/, "").trim(),
        url: String(item.url || "").trim(),
      };
    })
    .filter((item) => item.label);
}

onMounted(() => {
  try {
    recentKeywords.value = JSON.parse(localStorage.getItem("foodRecentKeywords") || "[]");
  } catch {
    recentKeywords.value = [];
  }
  const q = route.query.q ? String(route.query.q) : "";
  if (q) search(q);
});
</script>

<template>
  <main class="workspace food-layout">
    <section class="page-head food-search-hero">
      <span class="eyebrow">Food safety</span>
      <h1>음식 안전 검색</h1>
      <p>음식명을 입력하면 먹어도 되는지, 병원에 연락해야 하는지, 어떤 증상을 봐야 하는지 확인할 수 있습니다.</p>
    </section>

    <section class="food-search-panel">
      <p v-if="error && !searched" class="message error">{{ formatNotice(error) }}</p>
      <form class="food-search-box" @submit.prevent="search()">
        <div class="search-field-with-icon">
          <svg class="search-magnifier-icon" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          <input v-model="keyword" placeholder="예: 초콜릿, 포도, 자일리톨, 마늘" autofocus />
        </div>
        <button class="primary">검색</button>
      </form>
      <div class="food-quick-row" aria-label="추천 검색어">
        <span class="food-row-label">추천</span>
        <div class="food-inline-chip-list">
          <button v-for="item in quickKeywords.slice(0, 4)" :key="item" type="button" @click="search(item)">{{ item }}</button>
        </div>
      </div>
      <div v-if="recentKeywords.length" class="recent-search-panel">
        <span class="food-row-label recent-search-label">최근 검색</span>
        <div class="recent-search-list" aria-label="최근 검색어">
          <span v-for="item in recentKeywords" :key="item" class="recent-search-chip">
            <button type="button" class="recent-keyword" @click="search(item)">{{ item }}</button>
            <button type="button" class="remove-recent" :aria-label="`${item} 최근 검색 삭제`" title="삭제" @click.stop.prevent="removeRecentKeyword(item)">×</button>
          </span>
        </div>
        <button type="button" class="recent-clear-btn" @click="clearRecentKeywords">전체 삭제</button>
      </div>
    </section>

    <section v-if="searched" class="food-result-list">
      <div v-if="loading" class="recovery-panel loading">
        <strong>음식 안전 정보를 불러오는 중입니다.</strong>
        <p>{{ lastKeyword }} 검색 결과를 확인하고 있어요.</p>
      </div>

      <div v-else-if="error" class="recovery-panel error">
        <strong>음식 안전 정보를 불러오지 못했어요.</strong>
        <p>{{ error }}</p>
        <div class="recovery-actions">
          <button class="primary" type="button" @click="retrySearch">다시 시도</button>
          <button type="button" @click="resetSearch">검색 초기화</button>
        </div>
      </div>

      <template v-else>
        <article
          v-for="food in foods"
          :key="food.id"
          class="food-result-card"
          :class="riskClass(food)"
          role="button"
          tabindex="0"
          :aria-label="`${food.foodName} 상세 내용 보기`"
          @click="openFoodDetails(food)"
          @keydown.enter.prevent="openFoodDetails(food)"
          @keydown.space.prevent="openFoodDetails(food)"
        >
          <div class="card-head">
            <h2>{{ food.foodName }}</h2>
            <span class="status-pill">{{ food.riskLevel }}</span>
          </div>

          <dl class="food-summary-grid">
            <div v-for="item in summaryFields(food)" :key="item.label">
              <dt>{{ item.label }}</dt>
              <dd class="food-sentence-lines">
                <span v-for="line in splitSentenceLines(item.value)" :key="line">{{ line }}</span>
              </dd>
            </div>
          </dl>

          <div class="food-card-footer">
            <button
              class="food-detail-toggle"
              type="button"
              aria-haspopup="dialog"
              @click.stop="openFoodDetails(food)"
            >
              자세히 보기
            </button>
          </div>
          <strong v-if="food.immediateVet" class="danger-text">즉시 동물병원 상담 권장</strong>
        </article>
      </template>

      <div v-if="!loading && !error && foods.length === 0" class="recovery-panel empty">
        <strong>항목이 없어요.</strong>
        <p>아직 준비되지 않은 음식 정보일 수 있어요. 음식명, 별칭, 영문명을 다르게 입력해 보세요.</p>
        <div class="recovery-actions">
          <button class="primary" type="button" @click="retrySearch">다시 조회</button>
          <button type="button" @click="resetSearch">검색 초기화</button>
        </div>
      </div>
    </section>
    <section v-else class="food-result-list food-search-guide">
      <div class="recovery-panel empty">
        <strong>확인하고 싶은 음식을 검색해 보세요.</strong>
        <p>초콜릿, 포도, 닭가슴살처럼 음식 이름을 입력하면 급여 가능 여부와 주의할 증상을 확인할 수 있습니다.</p>
      </div>
    </section>

    <div v-if="selectedFood" class="food-detail-modal-backdrop" @click.self="closeFoodDetails">
      <section class="food-detail-modal" role="dialog" aria-modal="true" :aria-labelledby="`food-detail-title-${foodKey(selectedFood)}`">
        <header class="food-detail-modal-head" :class="riskClass(selectedFood)">
          <div>
            <span class="eyebrow">Food detail</span>
            <div class="food-detail-title-row">
              <h2 :id="`food-detail-title-${foodKey(selectedFood)}`">{{ selectedFood.foodName }}</h2>
              <span class="status-pill">{{ selectedFood.riskLevel }}</span>
            </div>
          </div>
          <button type="button" class="food-detail-modal-close" @click="closeFoodDetails">닫기</button>
        </header>

        <section :id="detailId(selectedFood)" class="food-detail-panel modal">
          <div v-for="section in detailSections(selectedFood)" :key="section.key" class="food-detail-content">
            <strong class="food-detail-section-title">
              <span class="food-detail-section-icon" :class="`icon-${section.icon}`" aria-hidden="true"></span>
              <span>{{ section.label }}</span>
            </strong>
            <p v-if="section.type === 'text'">{{ section.value }}</p>
            <div v-else-if="section.type === 'tags'" class="food-chip-row inline">
              <span v-for="tag in section.value" :key="tag" class="soft-chip" :class="tagClass(tag)">{{ tag }}</span>
            </div>
            <ul v-else-if="section.type === 'references'" class="food-reference-links compact">
              <li v-for="(item, index) in section.value" :key="`${item.label}-${index}`">
                <a v-if="item.url" :href="item.url" target="_blank" rel="noopener noreferrer">{{ item.label }}</a>
                <span v-else>{{ item.label }}</span>
              </li>
            </ul>
            <div v-else class="food-formatted-lines">
              <span v-for="line in foodSentenceLines(section.value, '-')" :key="line">{{ line }}</span>
            </div>
          </div>
        </section>

      </section>
    </div>
  </main>
</template>
