<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { emergencyApi } from "../api/resources";
import { useConfirm } from "../composables/useConfirm";
import { formatNotice } from "../utils/messageFormat";

const confirm = useConfirm();
const route = useRoute();
const checks = ref([]);
const selected = ref(null);
const vetReport = ref(null);
const loading = ref(false);
const error = ref("");
const currentPage = ref(0);
const riskSummaryOpen = ref(false);
const PAGE_SIZE = 8;

const guardianVetQuestions = [
  "우리 강아지의 현재 상태가 응급 진료가 필요한 수준인가요?",
  "지금 보이는 증상으로 어떤 문제들을 우선 확인해야 하나요?",
  "병원에 도착하기 전 집에서 해도 되는 행동과 피해야 할 행동은 무엇인가요?",
  "진료 시 어떤 검사나 처치가 필요할 수 있나요?",
  "집으로 돌아간 뒤 어떤 변화가 생기면 즉시 다시 연락하거나 내원해야 하나요?",
];

const totalPages = computed(() => Math.ceil(checks.value.length / PAGE_SIZE));
const pagedChecks = computed(() => {
  const start = currentPage.value * PAGE_SIZE;
  return checks.value.slice(start, start + PAGE_SIZE);
});
const canGoPrev = computed(() => currentPage.value > 0);
const canGoNext = computed(() => currentPage.value + 1 < totalPages.value);
const pageNumbers = computed(() => {
  const total = totalPages.value;
  if (total <= 5) return Array.from({ length: total }, (_, index) => index);
  const start = Math.max(0, Math.min(currentPage.value - 2, total - 5));
  return Array.from({ length: 5 }, (_, index) => start + index);
});

function loadErrorMessage(target = "리포트") {
  return `${target}를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.`;
}

function formatDateParts(value) {
  const text = String(value || "");
  if (!text) return { date: "-", time: "" };
  const [date, time = ""] = text.replace(" ", "T").split("T");
  return { date, time: time.slice(0, 8) };
}

async function load() {
  loading.value = true;
  error.value = "";
  try {
    checks.value = await emergencyApi.list();
    currentPage.value = 0;
    const reportId = Number(route.query.reportId || 0);
    const target = reportId ? checks.value.find((check) => Number(check.id) === reportId) : null;
    if (target) {
      const targetIndex = checks.value.findIndex((check) => Number(check.id) === reportId);
      currentPage.value = Math.max(0, Math.floor(targetIndex / PAGE_SIZE));
      await showReport(target);
    }
  } catch (e) {
    error.value = loadErrorMessage("응급 체크 리포트");
  } finally {
    loading.value = false;
  }
}

async function showReport(check) {
  selected.value = check;
  vetReport.value = null;
  riskSummaryOpen.value = false;
  error.value = "";
  try {
    vetReport.value = await emergencyApi.report(check.id);
  } catch (e) {
    error.value = loadErrorMessage("선택한 리포트");
  }
}

async function removeReport(check) {
  const ok = await confirm.ask({
    title: "리포트 삭제",
    message: "선택한 응급 체크 리포트를 삭제할까요? 삭제한 리포트는 복구할 수 없습니다.",
    confirmText: "삭제",
    tone: "danger",
  });
  if (!ok) return;

  error.value = "";
  try {
    await emergencyApi.remove(check.id);
    checks.value = checks.value.filter((item) => item.id !== check.id);
    if (currentPage.value > 0 && currentPage.value >= totalPages.value) {
      currentPage.value = Math.max(0, totalPages.value - 1);
    }
    if (selected.value?.id === check.id) {
      selected.value = null;
      vetReport.value = null;
    }
  } catch (e) {
    error.value = "리포트 삭제를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.";
  }
}

async function downloadVetReportPdf(id) {
  if (!id) return;
  error.value = "";
  try {
    await emergencyApi.downloadReportPdf(id);
  } catch (e) {
    error.value = e?.message || "PDF 다운로드를 완료하지 못했어요. 잠시 후 다시 시도해 주세요.";
  }
}

function changePage(page) {
  if (page < 0 || page >= totalPages.value || page === currentPage.value) return;
  currentPage.value = page;
}

function parseQuestions(value) {
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed.length ? parsed : guardianVetQuestions;
    return parsed ? [String(parsed)] : guardianVetQuestions;
  } catch {
    const questions = String(value || "").split("|").filter(Boolean);
    return questions.length ? questions : guardianVetQuestions;
  }
}

function riskClass(level) {
  const v = String(level || "").toLowerCase();
  const hasAny = (keywords) => keywords.some((keyword) => v.includes(keyword));
  if (hasAny(["정보부족", "정보 부족", "insufficient", "unknown"])) return "unknown";
  if (hasAny(["위험", "위급", "응급", "심각", "emergency", "danger", "high"])) return "urgent";
  if (hasAny(["주의", "중간", "caution", "medium"])) return "caution";
  if (hasAny(["관찰", "observe", "watch", "low"])) return "observe";
  return "safe";
}

function formatRiskSummary(value = "") {
  return String(value || "")
    .replace(/[ \t]+\n/g, "\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function shouldHideRiskSummaryLine(line = "") {
  return /상위 독소|우선 매칭|매칭 대상|룰과 연결|룰로 분류|응급 룰|위험 룰|룰 설계|근거 판단 룰|구조화된 증상 키워드|근거 도출|내부 로직|food_safety|emergency_rules/i.test(line);
}

function riskSummaryParagraphs(value = "") {
  return formatRiskSummary(value)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !shouldHideRiskSummaryLine(line))
    .map((line) => line.replace(/^(요약|응급도|판단 근거|권장 방향|지금 할 일|피해야 할 일)\s*:\s*/, ""))
    .filter(Boolean);
}

function riskSummaryRows(value = "") {
  const rows = [];
  let skippingEvidence = false;
  for (const rawLine of formatRiskSummary(value).split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || shouldHideRiskSummaryLine(line)) continue;
    if (/^참고\s*근거\s*요지\s*:/.test(line)) {
      skippingEvidence = true;
      continue;
    }
    const separator = line.indexOf(":");
    const hasLabel = separator > 0 && separator <= 18;
    if (skippingEvidence && !hasLabel) continue;
    if (hasLabel) skippingEvidence = false;
    if (hasLabel) {
      rows.push({
        label: line.slice(0, separator).trim(),
        value: line.slice(separator + 1).trim() || "-",
      });
    } else {
      rows.push({ label: "요약", value: line });
    }
  }
  return rows
    .filter((item) => item.value)
    .filter((item) => item.label !== "요약");
}

function evidenceItems(value = "") {
  try {
    const parsed = JSON.parse(value || "[]");
    return Array.isArray(parsed)
      ? parsed
        .map((item) => ({
          ...item,
          summary: shouldHideRiskSummaryLine(item?.summary || "") ? "" : item?.summary,
        }))
        .filter((item) => item?.source || item?.title || item?.summary)
      : [];
  } catch {
    return [];
  }
}

onMounted(load);
</script>

<template>
  <main class="workspace diary-layout">
    <section class="diary-hero report-hero">
      <div>
        <span class="eyebrow">Vet reports</span>
        <h1>응급 체크 리포트</h1>
        <p>이전 응급 체크 이력을 확인하고 수의사에게 전달할 PDF를 다운로드합니다.</p>
      </div>
      <button type="button" @click="load">새로고침</button>
    </section>

    <p v-if="error" class="message error">{{ formatNotice(error) }}</p>

    <section class="split-layout report-split-layout">
      <section class="table-panel">
        <div class="panel-head"><h2>응급 체크 이력</h2></div>
        <div v-if="!loading && checks.length > 0" class="report-list-footer">
          <span>총 {{ checks.length }}개</span>
          <span>{{ totalPages ? currentPage + 1 : 0 }} / {{ totalPages }} 페이지</span>
        </div>
        <table class="report-table">
          <colgroup>
            <col class="report-date-col" />
            <col class="report-pet-col" />
            <col class="report-risk-col" />
            <col class="report-symptom-col" />
            <col class="report-action-col" />
          </colgroup>
          <thead>
            <tr><th>일시</th><th>반려견</th><th>응급도</th><th>증상</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-if="loading" class="report-loading-row">
              <td colspan="5" class="empty-state">리포트 목록을 불러오는 중입니다.</td>
            </tr>
            <tr
              v-for="check in pagedChecks"
              :key="check.id"
              class="report-data-row"
              :class="{ selected: selected?.id === check.id }"
            >
              <td class="report-date-cell"><span>{{ formatDateParts(check.createdAt).date }}</span><span>{{ formatDateParts(check.createdAt).time }}</span></td>
              <td class="report-pet-name">{{ check.petName || "-" }}</td>
              <td><span class="risk-level small" :class="riskClass(check.riskLevel)">{{ check.riskLevel }}</span></td>
              <td class="report-symptom"><span class="report-symptom-text">{{ check.symptomNote }}</span></td>
              <td>
                <div class="report-row-actions">
                  <button @click="showReport(check)">리포트 확인</button>
                  <button class="text-action compact danger" type="button" @click="removeReport(check)">삭제하기</button>
                </div>
              </td>
            </tr>
            <tr v-if="!loading && checks.length === 0" class="report-empty-row">
              <td colspan="5" class="empty-state">항목이 없어요. 응급 체크를 완료하면 리포트가 이곳에 표시됩니다.</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!loading && totalPages > 1" class="case-pagination report-pagination">
          <button type="button" :disabled="!canGoPrev" @click="changePage(currentPage - 1)">이전</button>
          <button
            v-for="n in pageNumbers"
            :key="n"
            type="button"
            class="page-num"
            :class="{ current: n === currentPage }"
            @click="changePage(n)"
          >
            {{ n + 1 }}
          </button>
          <button type="button" :disabled="!canGoNext" @click="changePage(currentPage + 1)">다음</button>
        </div>
      </section>

      <section class="vet-report-panel">
        <template v-if="vetReport">
          <div class="panel-head clean"><h2>응급 체크 리포트</h2><button type="button" class="primary-link report-pdf-link-compact" @click="downloadVetReportPdf(selected.id)">수의사 리포트 PDF</button></div>
          <div class="ai-context-card report-context-card">
            <strong>응급 체크 안내 및 주의사항</strong>
            <span>선택한 반려견, 증상 메모, 의심 음식, 반복 횟수, 위험도 판단 결과를 요약해 수의사에게 전달할 문장으로 정리합니다.</span>
            <small>생성된 응답은 보호자 설명 보조 자료이며, 최종 진료 판단은 동물병원 진료를 기준으로 합니다.</small>
          </div>
          <div class="report-grid">
            <article><span>반려견</span><strong>{{ selected.petName || "미선택" }}</strong></article>
            <article><span>응급도</span><strong><span class="risk-level report-detail-risk" :class="riskClass(selected.riskLevel)">{{ selected.riskLevel }}</span></strong></article>
            <article><span>즉시 병원</span><strong>{{ selected.immediateVet ? "권장" : "상황 관찰" }}</strong></article>
            <article><span>의심 음식</span><strong>{{ selected.suspectedFoodName || selected.suspectedFoodText || "-" }}</strong></article>
          </div>
          <section v-if="riskSummaryRows(vetReport.riskSummary).length" class="result-input-summary risk-summary-toggle-card report-risk-summary-card">
            <div class="summary-toggle-head">
              <strong>위험 요약</strong>
              <button
                type="button"
                class="summary-toggle-button"
                :aria-expanded="riskSummaryOpen"
                @click="riskSummaryOpen = !riskSummaryOpen"
              >
                {{ riskSummaryOpen ? "접기" : "자세히 보기" }}
              </button>
            </div>
            <ul v-if="riskSummaryOpen" class="report-risk-summary-list">
              <li v-for="item in riskSummaryRows(vetReport.riskSummary)" :key="`${item.label}-${item.value}`" class="report-risk-summary-row">
                <b>{{ item.label }}:</b>
                <span :class="riskClass(item.value)">{{ item.value }}</span>
              </li>
            </ul>
          </section>
          <section class="report-note report-question-list">
            <b>수의사에게 할 질문</b>
            <ul>
              <li v-for="q in parseQuestions(vetReport.vetQuestions)" :key="q">{{ q }}</li>
            </ul>
          </section>
          <section v-if="evidenceItems(vetReport.evidenceSummary).length" class="report-note report-source-list">
            <b>참고 근거 출처</b>
            <ul>
              <li v-for="item in evidenceItems(vetReport.evidenceSummary)" :key="`${item.source}-${item.title}`">
                <a v-if="item.sourceUrl" class="report-source-title" :href="item.sourceUrl" target="_blank" rel="noopener noreferrer"><strong>{{ item.source || "근거" }} - {{ item.title || "제목 없음" }}</strong></a>
                <strong v-else>{{ item.source || "근거" }} - {{ item.title || "제목 없음" }}</strong>
                <span v-if="item.summary">{{ item.summary }}</span>
              </li>
            </ul>
          </section>
        </template>
        <div v-else class="panel-copy report-empty-guide">
          <div class="ai-context-card report-context-card">
            <strong>리포트를 선택해 주세요.</strong>
            <ol class="report-guide-steps">
              <li>왼쪽 목록에서 확인할 상담 이력을 찾습니다.</li>
              <li><b>리포트 확인</b>을 누르면 상세 내용이 열립니다.</li>
              <li>불러오지 못한 경우 새로고침 후 다시 시도해 주세요.</li>
            </ol>
          </div>
          <p>선택한 리포트의 AI 요약과 수의사 전달 항목이 이곳에 표시됩니다.</p>
        </div>
      </section>
    </section>
  </main>
</template>
