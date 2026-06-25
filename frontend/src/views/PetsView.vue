<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { petApi, petPlanApi } from "../api/resources";
import AppToast from "../components/AppToast.vue";
import CustomSelect from "../components/CustomSelect.vue";
import { useConfirm } from "../composables/useConfirm";
import { formatNotice } from "../utils/messageFormat";

const planCategoryOptions = ["병원 진료", "응급 체크", "약 복용", "예방접종", "검진", "케어 루틴"].map((c) => ({ value: c, label: c }));
const petSelectOptions = computed(() => [
  { value: "", label: "선택" },
  ...pets.value.map((p) => ({ value: p.id, label: p.name })),
]);

const petTabs = [
  { key: "pets", label: "반려견 등록", icon: "🐾" },
  { key: "plans", label: "관리 기록", icon: "📋" },
];
const genderStatusOptions = [
  { value: "MALE_INTACT", label: "남아" },
  { value: "FEMALE_INTACT", label: "여아" },
  { value: "MALE_NEUTERED", label: "남아 · 중성화" },
  { value: "FEMALE_NEUTERED", label: "여아 · 중성화" },
];
const activeTab = ref("plans");
const route = useRoute();
const confirm = useConfirm();

const pets = ref([]);
const plans = ref([]);
const editingId = ref(null);
const editingPlanId = ref(null);
const error = ref("");
const toast = reactive({ show: false, type: "error", message: "" });
let toastTimer = null;
const petKeyword = ref("");
const showPlanModal = ref(false);
const form = reactive({ name: "", breed: "", age: "", weight: "", genderStatus: "MALE_INTACT", allergies: "", diseases: "" });
const planForm = reactive({ petId: "", emergencyCheckId: null, title: "", category: "병원 진료", planDate: "", memo: "", completed: false });
const calendarCursor = ref(new Date());
const selectedCalendarDate = ref(formatDateValue(new Date()));
const selectedRecordPage = ref(1);
const selectedRecordPageSize = 4;
const completedPlanCount = computed(() => plans.value.filter((plan) => plan.completed).length);
const pendingPlanCount = computed(() => plans.value.filter((plan) => !plan.completed).length);
const planProgress = computed(() => {
  if (plans.value.length === 0) return 0;
  return Math.round((completedPlanCount.value / plans.value.length) * 100);
});
const nextPlan = computed(() => plans.value.find((plan) => !plan.completed) || null);
const filteredPets = computed(() => {
  const keyword = petKeyword.value.trim().toLowerCase();
  if (!keyword) return pets.value;
  return pets.value.filter((pet) => {
    return [pet.name, pet.breed, genderStatusLabel(pet), pet.allergies, pet.diseases]
      .some((value) => String(value || "").toLowerCase().includes(keyword));
  });
});
const calendarTitle = computed(() => {
  const year = calendarCursor.value.getFullYear();
  const month = calendarCursor.value.getMonth() + 1;
  return `${year}년 ${month}월`;
});
const selectedPlanDateInfo = computed(() => {
  if (!selectedCalendarDate.value) return "날짜를 선택해 주세요";
  const date = new Date(`${selectedCalendarDate.value}T00:00:00`);
  return {
    label: date.toLocaleDateString("ko-KR", { year: "numeric", month: "long", day: "numeric", weekday: "long" }),
    holidayName: holidayName(selectedCalendarDate.value),
  };
});
const selectedPlanDateLabel = computed(() => selectedPlanDateInfo.value?.label || "날짜를 선택해 주세요");
const selectedDatePlans = computed(() => plans.value.filter((plan) => plan.planDate === selectedCalendarDate.value));
const selectedDateItems = computed(() => {
  const items = [];
  const holiday = selectedPlanDateInfo.value?.holidayName;
  if (holiday) {
    items.push({
      id: `${selectedCalendarDate.value}-holiday`,
      title: holiday,
      category: "공휴일",
      isHoliday: true,
    });
  }
  return [...items, ...selectedDatePlans.value];
});
const selectedRecordTotalPages = computed(() => Math.max(1, Math.ceil(selectedDateItems.value.length / selectedRecordPageSize)));
const selectedRecordCurrentPage = computed(() => Math.min(selectedRecordPage.value, selectedRecordTotalPages.value));
const visibleSelectedDateItems = computed(() => {
  const start = (selectedRecordCurrentPage.value - 1) * selectedRecordPageSize;
  return selectedDateItems.value.slice(start, start + selectedRecordPageSize);
});
const latestPlan = computed(() => {
  return [...plans.value].sort((a, b) => String(b.planDate || "").localeCompare(String(a.planDate || "")))[0] || null;
});
const categoryToneMap = {
  "병원 방문": "hospital",
  "병원 진료": "hospital",
  "응급 체크": "emergency",
  "약 복용": "medicine",
  "예방접종": "vaccine",
  "검진": "checkup",
  "케어 루틴": "routine",
  "관찰 기록": "observe",
};
const maxCalendarChipCount = 2;
const calendarDays = computed(() => {
  const year = calendarCursor.value.getFullYear();
  const month = calendarCursor.value.getMonth();
  const first = new Date(year, month, 1);
  const last = new Date(year, month + 1, 0);
  const todayValue = formatDateValue(new Date());
  const selectedValue = selectedCalendarDate.value;
  const days = [];
  for (let i = 0; i < first.getDay(); i += 1) {
    days.push({ key: `blank-${i}`, label: "", value: "", blank: true });
  }
  for (let day = 1; day <= last.getDate(); day += 1) {
    const date = new Date(year, month, day);
    const value = formatDateValue(date);
    const dayPlans = plans.value.filter((plan) => plan.planDate === value);
    const dayHolidayName = holidayName(value);
    const holidayEvent = dayHolidayName ? [{ id: `${value}-holiday`, label: dayHolidayName, tone: "holiday" }] : [];
    const planEvents = dayPlans.map((plan) => ({
      id: plan.id,
      label: calendarEventLabel(plan),
      tone: categoryTone(plan.category),
    }));
    const calendarEvents = [...holidayEvent, ...planEvents];
    days.push({
      key: value,
      label: String(day),
      value,
      holidayName: dayHolidayName,
      holiday: Boolean(dayHolidayName),
      count: dayPlans.length,
      events: calendarEvents.slice(0, maxCalendarChipCount),
      extraCount: Math.max(calendarEvents.length - maxCalendarChipCount, 0),
      today: value === todayValue,
      selected: value === selectedValue,
    });
  }
  return days;
});

function commonErrorMessage(action = "요청을 처리") {
  return sentenceLines(`${action}하지 못했어요. 잠시 후 다시 시도해 주세요.`);
}

function sentenceLines(value) {
  return formatNotice(value);
}

function errorMessage(error, fallbackAction) {
  return sentenceLines(error?.message || commonErrorMessage(fallbackAction));
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

function formatDateValue(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function holidayName(value) {
  const fixedHolidays = {
    "01-01": "신정",
    "03-01": "삼일절",
    "05-05": "어린이날",
    "06-06": "현충일",
    "08-15": "광복절",
    "10-03": "개천절",
    "10-09": "한글날",
    "12-25": "성탄절",
  };
  const yearHolidays = {
    "2026-02-16": "설날 연휴",
    "2026-02-17": "설날",
    "2026-02-18": "설날 연휴",
    "2026-03-02": "대체공휴일",
    "2026-05-24": "부처님오신날",
    "2026-05-25": "대체공휴일",
    "2026-08-17": "대체공휴일",
    "2026-09-24": "추석 연휴",
    "2026-09-25": "추석",
    "2026-09-26": "추석 연휴",
    "2026-10-05": "대체공휴일",
  };
  return yearHolidays[value] || fixedHolidays[String(value).slice(5)] || "";
}

function setCalendarCursorFromValue(value) {
  if (!value) return;
  const date = new Date(`${value}T00:00:00`);
  if (!Number.isNaN(date.getTime())) {
    calendarCursor.value = new Date(date.getFullYear(), date.getMonth(), 1);
  }
}

function changeCalendarMonth(amount) {
  calendarCursor.value = new Date(calendarCursor.value.getFullYear(), calendarCursor.value.getMonth() + amount, 1);
}

function selectCalendarDay(day) {
  if (!day.value) return;
  selectedCalendarDate.value = day.value;
  selectedRecordPage.value = 1;
}

function syncCalendarFromInput() {
  setCalendarCursorFromValue(planForm.planDate);
  if (planForm.planDate) {
    selectedCalendarDate.value = planForm.planDate;
    selectedRecordPage.value = 1;
  }
}

function changeSelectedRecordPage(page) {
  selectedRecordPage.value = Math.min(Math.max(page, 1), selectedRecordTotalPages.value);
}

function reset() {
  editingId.value = null;
  Object.assign(form, { name: "", breed: "", age: "", weight: "", genderStatus: "MALE_INTACT", allergies: "", diseases: "" });
}

async function load() {
  error.value = "";
  try {
    pets.value = await petApi.list();
    if (pets.value.length === 0) {
      plans.value = [];
      error.value = "";
      return;
      error.value = "관리 기록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
      return;
      return;
    }
    plans.value = await petPlanApi.list();
    if (plans.value.length > 0 && selectedDatePlans.value.length === 0) {
      selectedCalendarDate.value = latestPlan.value.planDate;
      setCalendarCursorFromValue(latestPlan.value.planDate);
    }
  } catch (e) {
    if (pets.value.length > 0) {
      plans.value = [];
      error.value = "";
      return;
      error.value = "관리 기록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
      return;
      error.value = "관리 기록을 불러오지 못했어요. 반려견 정보는 정상적으로 불러왔습니다.";
      return;
    }
    pets.value = [];
    plans.value = [];
    error.value = "반려견 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
    return;
    error.value = "반려견 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
    return;
    error.value = commonErrorMessage("반려견 정보를 불러오지");
  }
}

function isOverdue(value) {
  if (!value) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const target = new Date(`${value}T00:00:00`);
  return target < today;
}

function planDateState(plan) {
  if (plan.completed) return "완료";
  return "";
}

function edit(pet) {
  editingId.value = pet.id;
  Object.assign(form, {
    name: pet.name,
    breed: pet.breed || "",
    age: pet.age ?? "",
    weight: pet.weight ?? "",
    genderStatus: toGenderStatus(pet.gender, pet.neutered),
    allergies: pet.allergies || "",
    diseases: pet.diseases || "",
  });
}

async function save() {
  error.value = "";
  if (!form.name.trim()) {
    error.value = "반려견 이름을 입력해 주세요.";
    return;
  }
  const genderPayload = fromGenderStatus(form.genderStatus);
  const body = {
    ...form,
    genderStatus: undefined,
    gender: genderPayload.gender,
    breed: form.breed || null,
    age: form.age === "" ? null : Number(form.age),
    weight: form.weight === "" ? null : Number(form.weight),
    neutered: genderPayload.neutered,
    allergies: form.allergies || null,
    diseases: form.diseases || null,
  };
  try {
    editingId.value ? await petApi.update(editingId.value, body) : await petApi.create(body);
    reset();
    await load();
  } catch (e) {
    error.value = errorMessage(e, "반려견 저장을 완료");
  }
}

function toGenderStatus(gender, neutered) {
  const normalized = String(gender || "UNKNOWN").toUpperCase();
  if (["F", "FEMALE"].includes(normalized)) return neutered ? "FEMALE_NEUTERED" : "FEMALE_INTACT";
  if (["M", "MALE"].includes(normalized)) return neutered ? "MALE_NEUTERED" : "MALE_INTACT";
  return "MALE_INTACT";
}

function fromGenderStatus(value) {
  return {
    gender: String(value).startsWith("FEMALE") ? "FEMALE" : "MALE",
    neutered: String(value).endsWith("NEUTERED"),
  };
}

function genderStatusLabel(pet) {
  return genderStatusOptions.find((option) => option.value === toGenderStatus(pet.gender, pet.neutered))?.label || "성별 미입력";
}

async function remove(id) {
  const ok = await confirm.ask({
    title: "반려견 정보 삭제",
    message: "등록된 반려견 정보를 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  try {
    await petApi.remove(id);
    await load();
  } catch (e) {
    error.value = errorMessage(e, "반려견 삭제를 완료");
  }
}

function resetPlan() {
  editingPlanId.value = null;
  Object.assign(planForm, { petId: pets.value[0]?.id || "", emergencyCheckId: null, title: "", category: "병원 진료", planDate: "", memo: "", completed: false });
}

function planCategoryLabel(category) {
  return category === "병원 방문" ? "병원 진료" : category;
}

function applyPlanDraftFromRoute() {
  if (route.query.tab !== "plans") return;
  activeTab.value = "plans";
  const category = planCategoryLabel(String(route.query.category || "관찰 기록"));
  if (!planCategoryOptions.some((option) => option.value === category)) {
    planCategoryOptions.push({ value: category, label: category });
  }
  Object.assign(planForm, {
    petId: route.query.petId ? Number(route.query.petId) : pets.value[0]?.id || "",
    emergencyCheckId: route.query.emergencyCheckId ? Number(route.query.emergencyCheckId) : null,
    title: String(route.query.title || ""),
    category,
    planDate: String(route.query.planDate || formatDateValue(new Date())),
    memo: String(route.query.memo || ""),
    completed: false,
  });
  selectedCalendarDate.value = planForm.planDate;
  setCalendarCursorFromValue(planForm.planDate);
  showPlanModal.value = true;
}

function categoryTone(category) {
  return categoryToneMap[planCategoryLabel(category)] || "default";
}

function calendarEventLabel(plan) {
  return plan.title || String(plan.memo || "").split(/\r?\n/)[0]?.trim() || planCategoryLabel(plan.category) || "기록";
}

function editPlan(plan) {
  editingPlanId.value = plan.id;
  Object.assign(planForm, {
    petId: plan.petId,
    emergencyCheckId: plan.emergencyCheckId ?? null,
    title: plan.title,
    category: planCategoryLabel(plan.category),
    planDate: plan.planDate,
    memo: plan.memo || "",
    completed: Boolean(plan.completed),
  });
  setCalendarCursorFromValue(plan.planDate);
  selectedCalendarDate.value = plan.planDate;
  showPlanModal.value = true;
}

function openPlanModal(date = selectedCalendarDate.value) {
  resetPlan();
  planForm.planDate = date || formatDateValue(new Date());
  syncCalendarFromInput();
  showPlanModal.value = true;
}

function closePlanModal() {
  showPlanModal.value = false;
  resetPlan();
}

async function savePlan() {
  error.value = "";
  if (!planForm.petId) {
    showToast("반려견을 선택해 주세요.");
    return;
  }
  if (!planForm.planDate) {
    showToast("기록 날짜를 선택해 주세요.");
    return;
  }
  if (!planForm.petId) {
    error.value = "반려견을 선택해 주세요.";
    return;
  }
  if (!planForm.planDate) {
    error.value = "기록 날짜를 선택해 주세요.";
    return;
  }
  const fallbackTitle = planForm.memo.trim().split(/\r?\n/)[0]?.trim() || planForm.category || "관리 기록";
  const body = {
    petId: Number(planForm.petId),
    emergencyCheckId: planForm.emergencyCheckId || null,
    title: planForm.title.trim() || fallbackTitle.slice(0, 160),
    category: planForm.category,
    planDate: planForm.planDate,
    memo: planForm.memo || null,
    completed: Boolean(planForm.completed),
  };
  try {
    editingPlanId.value ? await petPlanApi.update(editingPlanId.value, body) : await petPlanApi.create(body);
    selectedCalendarDate.value = body.planDate;
    setCalendarCursorFromValue(body.planDate);
    resetPlan();
    showPlanModal.value = false;
    await load();
  } catch (e) {
    error.value = errorMessage(e, "관리 기록 저장을 완료");
    showToast(error.value);
  }
}

async function togglePlan(plan) {
  error.value = "";
  try {
    await petPlanApi.updateCompleted(plan.id, !plan.completed);
    await load();
  } catch (e) {
    error.value = errorMessage(e, "관리 기록 상태 변경을 완료");
  }
}

async function removePlan(id) {
  const ok = await confirm.ask({
    title: "관리 기록 삭제",
    message: "관리 기록을 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  try {
    await petPlanApi.remove(id);
    await load();
  } catch (e) {
    error.value = errorMessage(e, "관리 기록 삭제를 완료");
  }
}

onMounted(async () => {
  activeTab.value = "plans";
  await load();
  resetPlan();
  applyPlanDraftFromRoute();
});
</script>

<template>
  <main class="workspace pet-layout">
    <AppToast
      :show="toast.show"
      :type="toast.type"
      :message="toast.message"
      @close="toast.show = false"
    />
    <!-- 페이지 헤더 -->
    <section class="page-head">
      <span class="eyebrow">{{ activeTab === "plans" ? "CARE RECORD" : "MY PETS" }}</span>
      <h1>{{ activeTab === "plans" ? "기록하기" : "반려견 관리" }}</h1>
      <p>{{ activeTab === "plans" ? "반려견의 케어 흐름을 한눈에 확인하세요." : "반려견 정보를 등록하고 응급 체크와 관리 기록에 연결하세요." }}</p>
    </section>

    <p v-if="error" class="message error">{{ formatNotice(error) }}</p>

    <!-- ── 반려견 섹션 ── -->
    <section v-if="false" class="pet-section-wrap pet-management-page">
      <form class="pet-form-card form-panel pet-entry-panel" novalidate @submit.prevent="save">
        <div class="panel-head">
          <div>
            <h2>{{ editingId ? "반려견 수정" : "반려견 등록" }}</h2>
          </div>
          <button type="button" @click="reset">초기화</button>
        </div>
        <div class="pet-form-body">
          <div class="pet-form-row">
            <label><span class="lbl">이름 <span class="req">*</span></span><input v-model="form.name" required placeholder="예: 초코" /></label>
            <label><span class="lbl">견종</span><input v-model="form.breed" maxlength="60" placeholder="예: 푸들" /></label>
          </div>
          <div class="pet-form-row">
            <label><span class="lbl">나이 (세)</span><input v-model="form.age" type="number" min="0" max="40" placeholder="0" /></label>
            <label><span class="lbl">체중 (kg)</span><input v-model="form.weight" type="number" min="0.1" max="120" step="0.1" placeholder="0.0" /></label>
          </div>
          <div class="wide">
            <span class="lbl">성별 / 중성화 여부</span>
            <div class="segmented-choice-grid">
              <button
                v-for="option in genderStatusOptions"
                :key="option.value"
                type="button"
                class="segmented-choice"
                :class="{ active: form.genderStatus === option.value }"
                @click="form.genderStatus = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <label><span class="lbl">알레르기</span><input v-model="form.allergies" maxlength="255" placeholder="없으면 비워두세요" /></label>
          <label><span class="lbl">기저질환</span><input v-model="form.diseases" maxlength="255" placeholder="없으면 비워두세요" /></label>
          <div class="pet-form-actions">
            <button class="primary" type="submit">{{ editingId ? "수정 저장" : "등록하기" }}</button>
          </div>
        </div>
      </form>

      <section class="pet-list-section pet-list-panel">
        <div class="pet-list-head">
          <h2>등록된 반려견 <span class="pet-count">{{ pets.length }}</span></h2>
          <div class="pet-list-tools">
            <input v-model.trim="petKeyword" type="search" placeholder="이름, 견종, 특이사항 검색" />
            <button type="button" @click="load">새로고침</button>
          </div>
        </div>
        <div v-if="filteredPets.length > 0" class="pet-card-grid">
          <article v-for="pet in filteredPets" :key="pet.id" class="pet-profile-card">
            <div class="pet-avatar">{{ pet.name.charAt(0) }}</div>
            <div class="pet-info">
              <strong>{{ pet.name }}</strong>
              <span class="pet-breed">{{ pet.breed || "견종 미입력" }}</span>
              <div class="pet-stats">
                <span v-if="pet.age != null">{{ pet.age }}세</span>
                <span v-if="pet.weight != null">{{ pet.weight }}kg</span>
                <span>{{ genderStatusLabel(pet) }}</span>
              </div>
              <div v-if="pet.allergies || pet.diseases" class="pet-notes">
                <span v-if="pet.allergies">알레르기: {{ pet.allergies }}</span>
                <span v-if="pet.diseases">기저질환: {{ pet.diseases }}</span>
              </div>
            </div>
            <div class="pet-card-actions">
              <button class="text-action compact" type="button" @click="edit(pet)">수정하기</button>
              <button class="text-action compact danger" type="button" @click="remove(pet.id)">삭제하기</button>
            </div>
          </article>
        </div>
        <p v-else class="empty-state">
          항목이 없어요.<br />
          {{ pets.length ? "검색어를 줄이거나 다른 특이사항으로 찾아보세요." : "왼쪽 폼에서 첫 반려견을 추가해 보세요." }}
        </p>
      </section>
    </section>

    <!-- ── 관리 기록 캘린더 ── -->
    <section class="pet-section-wrap care-record-layout care-record-page">
      <section class="pet-list-section record-board-section">
        <div class="record-calendar-panel">
          <div class="record-calendar">
            <div class="mini-calendar-head">
              <button type="button" aria-label="이전 달" @click="changeCalendarMonth(-1)">‹</button>
              <strong>{{ calendarTitle }}</strong>
              <button type="button" aria-label="다음 달" @click="changeCalendarMonth(1)">›</button>
            </div>
            <div class="mini-calendar-week">
              <span>일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span>토</span>
            </div>
            <div class="mini-calendar-grid record-calendar-grid">
              <button
                v-for="day in calendarDays"
                :key="day.key"
                type="button"
                :disabled="day.blank"
                :class="{ selected: day.selected, today: day.today, hasRecord: day.count > 0, holiday: day.holiday }"
                @click="selectCalendarDay(day)"
              >
                <span class="calendar-day-number">{{ day.label }}</span>
                <span v-if="day.events?.length" class="calendar-event-list">
                  <span
                    v-for="event in day.events"
                    :key="event.id"
                    class="calendar-event-chip"
                    :class="`tone-${event.tone}`"
                  >
                    {{ event.label }}
                  </span>
                  <span v-if="day.extraCount" class="calendar-more-text">+{{ day.extraCount }}</span>
                </span>
              </button>
            </div>
          </div>
        </div>
        <div class="selected-record-list record-list-below">
          <div class="record-list-head">
            <h2 :class="{ holiday: selectedPlanDateInfo.holidayName }">
              <span>{{ selectedPlanDateLabel }}</span>
            </h2>
            <button class="primary record-create-button" type="button" @click="openPlanModal()">관리 기록 등록</button>
          </div>
          <div class="record-items-panel">
            <article v-for="item in visibleSelectedDateItems" :key="item.id" class="selected-record-card" :class="{ done: item.completed, holiday: item.isHoliday }">
              <div>
                <div class="plan-title-row">
                  <span class="status-pill" :class="item.isHoliday ? 'tone-holiday' : `tone-${categoryTone(item.category)}`">{{ item.isHoliday ? "공휴일" : planCategoryLabel(item.category) }}</span>
                  <span v-if="!item.isHoliday && planDateState(item)" class="plan-state" :class="{ done: item.completed }">
                    {{ planDateState(item) }}
                  </span>
                </div>
                <h3>{{ item.title }}</h3>
                <p v-if="!item.isHoliday">{{ item.petName }} · {{ item.memo || "메모 없음" }}</p>
              </div>
              <div v-if="!item.isHoliday" class="row-actions">
                <button class="text-action compact" type="button" @click="editPlan(item)">수정하기</button>
                <button class="text-action compact danger" type="button" @click="removePlan(item.id)">삭제하기</button>
              </div>
            </article>
            <div v-if="plans.length === 0 && selectedDateItems.length === 0" class="record-empty-card record-empty-card--all">
              <span class="record-empty-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M7 3v3M17 3v3M4.5 9.2h15" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <rect x="4.5" y="5.2" width="15" height="15.3" rx="3.2" stroke="currentColor" stroke-width="1.8" />
                  <path d="M8.2 13.5h4.6M8.2 16.6h7.6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </span>
              <strong>아직 관리 기록이 없어요.</strong>
              <span>진료, 약 복용, 예방접종 같은 일정을 남기면 달력에 보기 좋게 쌓여요.</span>
              <button class="record-empty-action" type="button" @click="openPlanModal()">첫 기록 등록하기</button>
            </div>
            <div v-else-if="selectedDateItems.length === 0" class="record-empty-card record-empty-card--date">
              <span class="record-empty-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none">
                  <path d="M12 6.5v5.8l3.6 2.1" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <circle cx="12" cy="12" r="8" stroke="currentColor" stroke-width="1.8" />
                  <path d="M18.5 5.5 20 4M5.5 5.5 4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </span>
              <strong>이 날짜에는 기록이 없어요.</strong>
              <span>선택한 날짜에 맞춰 새 기록을 남기거나, 기록 표시가 있는 날짜를 눌러보세요.</span>
              <button class="record-empty-action secondary" type="button" @click="openPlanModal()">이 날짜에 기록 추가</button>
            </div>
          </div>
          <div v-if="selectedDateItems.length > selectedRecordPageSize" class="record-pagination" aria-label="선택 날짜 기록 페이지">
            <button type="button" :disabled="selectedRecordCurrentPage <= 1" @click="changeSelectedRecordPage(selectedRecordCurrentPage - 1)">이전</button>
            <button
              v-for="page in selectedRecordTotalPages"
              :key="page"
              type="button"
              :class="{ active: page === selectedRecordCurrentPage }"
              @click="changeSelectedRecordPage(page)"
            >
              {{ page }}
            </button>
            <button type="button" :disabled="selectedRecordCurrentPage >= selectedRecordTotalPages" @click="changeSelectedRecordPage(selectedRecordCurrentPage + 1)">다음</button>
          </div>
        </div>
      </section>
    </section>

    <div v-if="showPlanModal" class="record-modal-backdrop" @click.self="closePlanModal">
      <form class="record-modal pet-form-card form-panel record-form-card" novalidate @submit.prevent="savePlan">
        <div class="panel-head">
          <div>
            <span class="eyebrow">CARE RECORD</span>
            <h2>{{ editingPlanId ? "관리 기록 수정" : "관리 기록 등록" }}</h2>
          </div>
          <button type="button" @click="closePlanModal">닫기</button>
        </div>
        <div class="pet-form-body">
          <div class="pet-form-row">
            <label>
              <span class="lbl">반려견 <span class="req">*</span></span>
              <CustomSelect v-model="planForm.petId" :options="petSelectOptions" aria-label="반려견" />
            </label>
            <label>
              <span class="lbl">카테고리</span>
              <CustomSelect v-model="planForm.category" :options="planCategoryOptions" aria-label="카테고리" />
            </label>
          </div>
          <div class="pet-form-row">
            <label>
              <span class="lbl">기록 날짜 <span class="req">*</span></span>
              <input v-model="planForm.planDate" type="date" required @change="syncCalendarFromInput" />
            </label>
            <label><span class="lbl">제목</span><input v-model="planForm.title" maxlength="160" placeholder="비워두면 메모나 카테고리로 저장돼요" /></label>
          </div>
          <label><span class="lbl">메모</span><textarea v-model="planForm.memo" rows="4" maxlength="500" placeholder="준비물이나 병원 전달 내용을 적어주세요" /></label>
          <div class="pet-form-actions">
            <label v-if="editingPlanId" class="check-line"><input v-model="planForm.completed" type="checkbox" /> 완료됨</label>
            <button class="primary" type="submit">{{ editingPlanId ? "수정 저장" : "기록 저장" }}</button>
          </div>
        </div>
      </form>
    </div>
  </main>
</template>
