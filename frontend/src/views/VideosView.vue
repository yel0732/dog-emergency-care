<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { videoApi } from "../api/resources";
import { useAuthStore } from "../stores/auth";
import CustomSelect from "../components/CustomSelect.vue";
import AppToast from "../components/AppToast.vue";
import { useConfirm } from "../composables/useConfirm";
import { VIDEO_SORT_OPTIONS, optionValues } from "../constants/queryOptions";
import { createMutationResync } from "../utils/stateRecovery";
import { formatNotice } from "../utils/messageFormat";

const sortOptions = VIDEO_SORT_OPTIONS;
const PAGE_SIZE = 9;
const DEFAULT_VIDEO_CATEGORY = "응급상황 대처";
const VIDEO_CATEGORY_NAMES = [
  "CPR/심폐소생술",
  "응급상황 대처",
  "발작/경련",
  "기도폐쇄/하임리히",
  "위험신호/건강체크",
  "구토/설사/소화기 증상",
  "이물섭취/위험물질",
  "호흡기 증상",
  "음식주의/중독",
  "약 복용/투약법",
];
const videoCategoryOptions = [
  { value: "", label: "선택" },
  ...VIDEO_CATEGORY_NAMES.map((c) => ({ value: c, label: c })),
];

const auth = useAuthStore();
const confirm = useConfirm();
const route = useRoute();
const router = useRouter();
const videos = ref([]);
const reviews = ref([]);
const selected = ref(null);
const editingId = ref(null);
const editingReviewId = ref(null);
const isVideoFormOpen = ref(false);
const error = ref("");
const loading = ref(false);
const loadError = ref("");
const notice = ref("");
const pageInfo = reactive({ total: 0, page: 0, size: PAGE_SIZE, totalPages: 0 });

const videoCategories = [
  { name: "CPR/심폐소생술", description: "심정지 상황, CPR 절차, 심폐소생술 실습" },
  { name: "응급상황 대처", description: "전반적인 응급상황 대응법" },
  { name: "발작/경련", description: "발작 증상과 보호자 대처법" },
  { name: "기도폐쇄/하임리히", description: "목 이물질, 하임리히법" },
  { name: "위험신호/건강체크", description: "아플 때 보이는 이상 신호" },
  { name: "구토/설사/소화기 증상", description: "구토·설사 등 소화기 증상과 대처법" },
  { name: "이물섭취/위험물질", description: "이물질 섭취와 위험 물질 노출 주의" },
  { name: "호흡기 증상", description: "기침, 역재채기, 리버스스니징" },
  { name: "음식주의/중독", description: "금지 음식, 음식 중독" },
  { name: "약 복용/투약법", description: "약 먹이는 방법" },
];

const filter = reactive({
  category: "",
  keyword: "",
  bookmarkedOnly: false,
  sort: "latest",
  direction: "desc",
});
const form = reactive({ title: "", category: "", symptom: "", youtubeUrl: "", description: "" });
const reviewForm = reactive({ rating: 5, content: "" });
const editingReviewForm = reactive({ rating: 5, content: "" });

const isAdmin = computed(() => auth.me?.role === "ADMIN");
const isReviewModalOpen = computed(() => Boolean(selected.value));
const videoFormTitle = computed(() => (editingId.value ? "영상 수정" : "영상 등록"));
const canGoPrev = computed(() => pageInfo.page > 0);
const canGoNext = computed(() => pageInfo.page + 1 < pageInfo.totalPages);
const hasActiveFilter = computed(() => Boolean(filter.category || filter.keyword || filter.bookmarkedOnly || filter.sort !== "latest"));
const currentUserReview = computed(() => reviews.value.find((review) => isMineReview(review)) || null);
const pageNumbers = computed(() => {
  const total = pageInfo.totalPages;
  if (total <= 1) return [];
  const start = Math.max(0, Math.min(pageInfo.page - 2, total - 5));
  const end = Math.min(total, start + 5);
  const arr = [];
  for (let i = start; i < end; i += 1) arr.push(i);
  return arr;
});

const sortValues = optionValues(sortOptions);
let syncingQuery = false;
let noticeTimer = null;

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : value;
}

function safeString(value) {
  return firstQueryValue(value) ? String(firstQueryValue(value)).trim() : "";
}

function safeOption(value, allowed, fallback) {
  const next = safeString(value);
  return allowed.has(next) ? next : fallback;
}

function safePage(value) {
  const next = Number(firstQueryValue(value));
  return Number.isInteger(next) && next >= 0 ? next : 0;
}

function isNotFound(errorObject) {
  return errorObject?.status === 404 || /404|not.?found/i.test(errorObject?.message || "");
}

const recoverVideoMutation = createMutationResync({
  reload: () => load(pageInfo.page, { syncUrl: false }),
  refreshDetail: async (videoId) => {
    if (videoId && selected.value?.id === videoId) {
      selected.value = await videoApi.find(videoId);
    }
  },
  isDetailMissing: isNotFound,
  onDetailMissing: closeReviewModal,
  setMessage: (nextMessage) => {
    error.value = nextMessage || error.value;
  },
});

function sameQuery(a, b) {
  const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
  for (const key of keys) {
    if (safeString(a[key]) !== safeString(b[key])) return false;
  }
  return true;
}

function youtubeEmbedUrl(url) {
  const id = youtubeId(url);
  return id ? `https://www.youtube.com/embed/${id}` : "";
}

function youtubeThumbnailUrl(url) {
  const id = youtubeId(url);
  return id ? `https://img.youtube.com/vi/${id}/mqdefault.jpg` : "";
}

function youtubeId(url) {
  if (!url) return "";
  const patterns = [
    /youtu\.be\/([^?&/]+)/,
    /youtube\.com\/watch\?v=([^?&/]+)/,
    /youtube\.com\/embed\/([^?&/]+)/,
    /youtube\.com\/shorts\/([^?&/]+)/,
  ];
  const match = patterns.map((pattern) => url.match(pattern)).find(Boolean);
  return match?.[1] || "";
}

const categoryIconMap = {
  "CPR/심폐소생술": "❤️",
  "응급상황 대처": "🚨",
  "발작/경련": "⚡",
  "기도폐쇄/하임리히": "🫁",
  "위험신호/건강체크": "🔍",
  "구토/설사/소화기 증상": "🤢",
  "이물섭취/위험물질": "⚠️",
  "호흡기 증상": "🌬️",
  "음식주의/중독": "🚫",
  "약 복용/투약법": "💉",
};
function categoryIcon(name) {
  return categoryIconMap[name] || "📌";
}

function categoryTone(category = "") {
  if (/CPR|응급상황|이물/.test(category)) return "tone-emergency";
  if (/중독|음식주의|위험물질/.test(category)) return "tone-toxic";
  if (/호흡|기도폐쇄|하임리히/.test(category)) return "tone-breath";
  if (/구토|설사/.test(category)) return "tone-digest";
  if (/약 복용|투약/.test(category)) return "tone-care";
  return "tone-check";
}

function isMineReview(review) {
  return Number(review.userId) === Number(auth.me?.id);
}

function canDeleteReview(review) {
  return isAdmin.value || isMineReview(review);
}

function reviewRating(review) {
  const value = Number(review?.rating);
  return Number.isFinite(value) && value >= 1 ? value : 5;
}

function resetVideo() {
  editingId.value = null;
  Object.assign(form, { title: "", category: "", symptom: "", youtubeUrl: "", description: "" });
}

function showNotice(message) {
  notice.value = message;
  if (noticeTimer) clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => {
    notice.value = "";
    noticeTimer = null;
  }, 3200);
}

function closeNotice() {
  notice.value = "";
  if (noticeTimer) {
    clearTimeout(noticeTimer);
    noticeTimer = null;
  }
}

function inferVideoCategory() {
  const text = `${form.title} ${form.symptom} ${form.description}`.toLowerCase();
  if (/cpr|심폐|심정지/.test(text)) return "CPR/심폐소생술";
  if (/발작|경련|떨림/.test(text)) return "발작/경련";
  if (/기도|하임리히|목.*막|숨.*막|이물|삼켰|삼킴|중독|위험물질|독성/.test(text)) return "이물섭취/위험물질";
  if (/기침|호흡|숨|역재채기|콧물/.test(text)) return "호흡기 증상";
  if (/구토|토하|토했|설사|혈변|묽은/.test(text)) return "구토/설사/소화기 증상";
  if (/약|투약|복용/.test(text)) return "약 복용/투약법";
  if (/초콜릿|포도|양파|과일|음식|먹|섭취|중독|위험물질|독성|금지/.test(text)) return "음식주의/중독";
  return DEFAULT_VIDEO_CATEGORY;
}

function ensureVideoCategory() {
  if (!form.category.trim()) {
    form.category = inferVideoCategory();
  }
}

function openCreateVideo() {
  if (!isAdmin.value) return;
  resetVideo();
  isVideoFormOpen.value = true;
}

function openEditVideo(video) {
  if (!isAdmin.value) return;
  editingId.value = video.id;
  Object.assign(form, {
    title: video.title,
    category: video.category,
    symptom: video.symptom || "",
    youtubeUrl: video.youtubeUrl,
    description: video.description || "",
  });
  isVideoFormOpen.value = true;
}

function closeVideoForm() {
  isVideoFormOpen.value = false;
  resetVideo();
}

function buildParams(page = pageInfo.page) {
  return {
    category: filter.category,
    keyword: filter.keyword,
    sort: filter.sort,
    direction: filter.direction,
    page,
    size: PAGE_SIZE,
  };
}

function buildQuery(page) {
  const query = {};
  if (filter.keyword) query.keyword = filter.keyword;
  if (filter.category) query.category = filter.category;
  if (filter.sort !== "latest") query.sort = filter.sort;
  if (filter.direction !== "desc") query.direction = filter.direction;
  if (filter.bookmarkedOnly) query.bookmarked = "1";
  if (page > 0) query.page = String(page);
  return query;
}

function videoPageRequest(page) {
  const params = buildParams(page);
  return filter.bookmarkedOnly ? videoApi.bookmarks(params) : videoApi.list(params);
}

function applyPage(response) {
  videos.value = response.items || [];
  Object.assign(pageInfo, {
    total: response.total || 0,
    page: response.page || 0,
    size: response.size || PAGE_SIZE,
    totalPages: response.totalPages || 0,
  });
}

function syncQueryToUrl(page) {
  const query = buildQuery(page);
  if (!sameQuery(route.query, query)) {
    syncingQuery = true;
    router.replace({ query }).catch(() => {}).finally(() => {
      syncingQuery = false;
    });
  }
}

function applyQueryFromUrl() {
  const q = route.query;
  filter.keyword = safeString(q.keyword);
  filter.category = safeString(q.category);
  filter.sort = safeOption(q.sort, sortValues, "latest");
  filter.direction = "desc";
  filter.bookmarkedOnly = safeString(q.bookmarked) === "1";
  return safePage(q.page);
}

async function load(page = 0, options = {}) {
  error.value = "";
  loadError.value = "";
  loading.value = true;
  const requestedPage = safePage(page);
  try {
    let response = await videoPageRequest(requestedPage);
    if ((response.items || []).length === 0 && filter.category && filter.keyword && !filter.bookmarkedOnly) {
      const keywordFallback = filter.keyword;
      filter.keyword = "";
      const categoryOnlyResponse = await videoPageRequest(0);
      if ((categoryOnlyResponse.items || []).length > 0) {
        response = categoryOnlyResponse;
      } else {
        filter.keyword = keywordFallback;
      }
    }
    if ((response.items || []).length === 0 && requestedPage > 0 && response.totalPages > 0) {
      response = await videoPageRequest(response.totalPages - 1);
    }
    applyPage(response);
    if (options.syncUrl !== false) syncQueryToUrl(pageInfo.page);
  } catch (e) {
    loadError.value = "영상 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
  } finally {
    loading.value = false;
  }
}

function search() {
  load(0);
}

function searchFromSubmit() {
  error.value = "";
  if (!filter.keyword.trim() && !filter.category && !filter.bookmarkedOnly) {
    error.value = "검색어를 입력하거나 카테고리 또는 찜한 영상 조건을 선택해 주세요.";
    return;
  }
  search();
}

function resetFilters() {
  Object.assign(filter, {
    category: "",
    keyword: "",
    bookmarkedOnly: false,
    sort: "latest",
    direction: "desc",
  });
  load(0);
}

function retryLoad() {
  load(pageInfo.page, { syncUrl: false });
}

function selectCategory(category) {
  filter.category = category;
  search();
}

function changePage(nextPage) {
  if (nextPage < 0 || nextPage >= pageInfo.totalPages) return;
  load(nextPage);
}

async function saveVideo() {
  if (!isAdmin.value) return;
  error.value = "";
  ensureVideoCategory();
  if (!form.title.trim() || !form.category.trim() || !form.youtubeUrl.trim()) {
    await confirm.ask({
      title: "입력을 확인해 주세요",
      message: "제목, 카테고리, YouTube URL은 반드시 입력해 주세요.",
      confirmText: "확인",
      cancelText: "닫기",
      tone: "danger",
    });
    return;
  }
  if (!youtubeId(form.youtubeUrl)) {
    await confirm.ask({
      title: "영상 주소를 확인해 주세요",
      message: "올바른 YouTube 영상 주소를 입력해 주세요.",
      confirmText: "확인",
      cancelText: "닫기",
      tone: "danger",
    });
    return;
  }
  const body = {
    title: form.title.trim(),
    category: form.category.trim(),
    symptom: form.symptom || null,
    youtubeUrl: form.youtubeUrl.trim(),
    description: form.description.trim() || null,
  };

  try {
    const saved = editingId.value ? await videoApi.update(editingId.value, body) : await videoApi.create(body);
    closeVideoForm();
    await load(pageInfo.page);
    await openVideo(saved);
  } catch (e) {
    await confirm.ask({
      title: "영상을 저장하지 못했습니다",
      message: e.message || "잠시 후 다시 시도해 주세요.",
      confirmText: "확인",
      cancelText: "닫기",
      tone: "danger",
    });
  }
}

async function removeVideo(video) {
  if (!isAdmin.value) return;
  const ok = await confirm.ask({
    title: "영상 삭제",
    message: "영상과 연결된 댓글이 함께 삭제됩니다. 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  try {
    await videoApi.remove(video.id);
    if (selected.value?.id === video.id) closeReviewModal();
    // 페이지의 마지막 항목을 지웠다면 이전 페이지로 복귀
    const lastOnPage = videos.value.length <= 1 && pageInfo.page > 0;
    await load(lastOnPage ? pageInfo.page - 1 : pageInfo.page);
  } catch (e) {
    error.value = e.message;
  }
}

async function openVideo(video) {
  error.value = "";
  try {
    const detail = await videoApi.find(video.id);
    if (!detail || !detail.id) {
      closeReviewModal();
      error.value = "영상을 찾을 수 없습니다. 목록을 다시 불러왔습니다.";
      await load(pageInfo.page);
      return;
    }
    selected.value = detail;
    resetReview();
    reviews.value = (await videoApi.reviews(video.id)) || [];
  } catch (e) {
    if (isNotFound(e)) {
      closeReviewModal();
      error.value = "영상을 찾을 수 없습니다. 목록을 다시 불러왔습니다.";
    } else {
      error.value = e.message;
    }
    await load(pageInfo.page);
  }
}

function closeReviewModal() {
  selected.value = null;
  reviews.value = [];
  resetReview();
}

async function toggleBookmark(video) {
  error.value = "";
  notice.value = "";
  try {
    const updated = video.bookmarked ? await videoApi.unbookmark(video.id) : await videoApi.bookmark(video.id);
    Object.assign(video, updated);
    if (selected.value?.id === video.id) {
      selected.value = { ...selected.value, ...updated };
    }
    if (filter.bookmarkedOnly && !updated.bookmarked) {
      await load(pageInfo.page);
    }
    showNotice(updated.bookmarked ? "찜한 영상에 추가했습니다." : "찜한 영상에서 해제했습니다.");
  } catch (e) {
    error.value = e.message;
    await recoverVideoMutation(error.value, video.id);
  }
}

async function toggleSelectedBookmark() {
  if (!selected.value) return;
  error.value = "";
  notice.value = "";
  try {
    const updated = selected.value.bookmarked
      ? await videoApi.unbookmark(selected.value.id)
      : await videoApi.bookmark(selected.value.id);
    selected.value = { ...selected.value, ...updated };
    videos.value = videos.value.map((video) => (video.id === updated.id ? { ...video, ...updated } : video));
    if (filter.bookmarkedOnly && !updated.bookmarked) {
      await load(pageInfo.page);
    }
    showNotice(updated.bookmarked ? "찜한 영상에 추가했습니다." : "찜한 영상에서 해제했습니다.");
  } catch (e) {
    error.value = e.message;
    await recoverVideoMutation(error.value, selected.value?.id);
  }
}

function resetReview() {
  editingReviewId.value = null;
  Object.assign(editingReviewForm, { rating: 5, content: "" });
  Object.assign(reviewForm, { rating: 5, content: "" });
}

function editReview(review) {
  editingReviewId.value = review.id;
  Object.assign(editingReviewForm, { rating: Number(review.rating || 5), content: review.content });
}

async function saveReview() {
  if (!selected.value) return;
  error.value = "";
  if (!reviewForm.content.trim()) {
    error.value = "댓글을 입력해 주세요.";
    return;
  }
  const body = { rating: Number(reviewForm.rating || 5), content: reviewForm.content };
  try {
    await videoApi.createReview(selected.value.id, body);
    resetReview();
    reviews.value = await videoApi.reviews(selected.value.id);
    selected.value = await videoApi.find(selected.value.id);
    videos.value = videos.value.map((video) => (video.id === selected.value.id ? { ...video, ...selected.value } : video));
  } catch (e) {
    if (isNotFound(e)) {
      closeReviewModal();
      error.value = "댓글 대상 영상을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
  }
}

async function saveInlineReview(review) {
  if (!selected.value || editingReviewId.value !== review.id) return;
  error.value = "";
  if (!editingReviewForm.content.trim()) {
    error.value = "댓글을 입력해 주세요.";
    return;
  }
  const body = { rating: Number(editingReviewForm.rating || 5), content: editingReviewForm.content };
  try {
    const updated = await videoApi.updateReview(review.id, body);
    reviews.value = reviews.value.map((item) => (item.id === updated.id ? { ...item, ...updated } : item));
    resetReview();
    const refreshedReviews = await videoApi.reviews(selected.value.id);
    reviews.value = refreshedReviews.map((item) => (item.id === updated.id ? { ...item, ...updated } : item));
    selected.value = await videoApi.find(selected.value.id);
    videos.value = videos.value.map((video) => (video.id === selected.value.id ? { ...video, ...selected.value } : video));
  } catch (e) {
    if (isNotFound(e)) {
      closeReviewModal();
      error.value = "댓글 대상 영상을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
  }
}

async function removeReview(id) {
  if (!selected.value) return;
  const ok = await confirm.ask({
    title: "댓글 삭제",
    message: "삭제한 댓글은 복구할 수 없습니다. 삭제할까요?",
    confirmText: "삭제",
    tone: "danger",
  });
  if (!ok) return;
  try {
    await videoApi.removeReview(id);
    reviews.value = await videoApi.reviews(selected.value.id);
    selected.value = await videoApi.find(selected.value.id);
    videos.value = videos.value.map((video) => (video.id === selected.value.id ? { ...video, ...selected.value } : video));
  } catch (e) {
    if (isNotFound(e)) {
      closeReviewModal();
      error.value = "댓글 대상 영상을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
  }
}

onMounted(() => {
  const startPage = applyQueryFromUrl();
  load(startPage);
});

watch(
  () => route.query,
  () => {
    if (syncingQuery) return;
    const startPage = applyQueryFromUrl();
    load(startPage);
  }
);
</script>

<template>
  <main class="workspace video-workspace">
    <section class="page-head video-page-head">
      <span class="eyebrow">Emergency videos</span>
      <h1>응급처치 영상</h1>
      <p>상황별 응급처치 가이드를 영상으로 확인하고, 자주 볼 영상은 하트로 찜할 수 있습니다.</p>
      <AppToast :show="Boolean(notice)" type="success" :message="notice" @close="closeNotice" />
      <p v-if="error" class="message error">{{ formatNotice(error) }}</p>
    </section>

    <section class="video-card-section">
      <div class="video-card-toolbar">
        <div class="video-toolbar-head">
          <div>
            <span class="eyebrow">Video finder</span>
            <h2>영상 검색</h2>
          </div>
          <button v-if="isAdmin" class="primary video-admin-create" type="button" @click="openCreateVideo">+ 영상 등록</button>
        </div>
        <div class="video-toolbar-actions">
          <form class="video-filter-form" novalidate @submit.prevent="searchFromSubmit">
            <div class="filter-search">
              <span class="filter-search-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
              </span>
              <input v-model="filter.keyword" placeholder="영상 제목·증상 검색" />
              <button class="filter-search-btn primary" type="submit">검색</button>
            </div>
            <div class="filter-options">
              <CustomSelect v-model="filter.sort" :options="sortOptions" aria-label="정렬 기준" @change="search" />
              <button
                class="bookmark-filter"
                :class="{ active: filter.bookmarkedOnly }"
                type="button"
                @click="filter.bookmarkedOnly = !filter.bookmarkedOnly; search()"
              >
                {{ filter.bookmarkedOnly ? "전체 영상 보기" : "찜한 영상 보기" }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <div class="video-category-grid" aria-label="영상 카테고리">
        <button type="button" class="video-cat-pill" :class="{ active: filter.category === '' }" @click="selectCategory('')">
          <span class="cat-icon">📋</span>전체
        </button>
        <button
          v-for="category in videoCategories"
          :key="category.name"
          type="button"
          class="video-cat-pill"
          :class="{ active: filter.category === category.name }"
          :title="category.description"
          @click="selectCategory(category.name)"
        >
          <span class="cat-icon">{{ categoryIcon(category.name) }}</span>{{ category.name }}
        </button>
      </div>

      <div class="video-list-summary">
        <span>총 {{ pageInfo.total }}개</span>
        <span>{{ pageInfo.totalPages ? pageInfo.page + 1 : 0 }} / {{ pageInfo.totalPages }} 페이지</span>
      </div>

      <div v-if="loading" class="recovery-panel loading">
        <strong>영상 목록을 불러오는 중입니다.</strong>
        <p>검색 조건과 페이지 정보를 확인하고 있어요.</p>
      </div>

      <div v-else-if="loadError" class="recovery-panel error">
        <strong>영상 목록을 불러오지 못했어요.</strong>
        <p>{{ loadError }}</p>
        <div class="recovery-actions">
          <button class="primary" type="button" @click="retryLoad">다시 시도</button>
          <button type="button" @click="resetFilters">조건 초기화</button>
        </div>
      </div>

      <div v-else class="video-card-grid">
        <article
          v-for="video in videos"
          :key="video.id"
          class="video-select-card video-embed-card"
          :class="categoryTone(video.category)"
          role="button"
          tabindex="0"
          :aria-label="`${video.title} 상세 보기`"
          @click="openVideo(video)"
          @keydown.enter.prevent="openVideo(video)"
          @keydown.space.prevent="openVideo(video)"
        >
          <button type="button" class="video-thumb-button" @click.stop="openVideo(video)">
            <img v-if="youtubeThumbnailUrl(video.youtubeUrl)" :src="youtubeThumbnailUrl(video.youtubeUrl)" :alt="video.title" />
            <span v-else class="video-thumb-fallback">영상</span>
            <span class="video-play-badge" aria-hidden="true">▶</span>
          </button>

          <div class="video-card-body">
            <div class="video-card-topline">
              <span class="status-pill video-category-pill">{{ video.category }}</span>
              <button
                class="video-bookmark-button"
                :class="{ saved: video.bookmarked }"
                type="button"
                :aria-label="video.bookmarked ? '찜 해제' : '찜하기'"
                :title="video.bookmarked ? '찜 해제' : '찜하기'"
                @click.stop="toggleBookmark(video)"
              >
                <span class="heart-icon" aria-hidden="true"></span>
              </button>
            </div>
            <strong>{{ video.title }}</strong>
            <small>{{ video.symptom || "증상 정보 없음" }}</small>
            <p>{{ video.description || "등록된 설명이 없습니다." }}</p>
            <div class="video-card-metrics">
              <span>댓글 {{ video.reviewCount || 0 }}</span>
              <span>평균 {{ Number(video.averageRating || 0).toFixed(1) }}</span>
              <span>찜 {{ video.bookmarkCount || 0 }}</span>
            </div>
          </div>

          <div class="video-card-actions">
            <button class="primary" type="button" @click.stop="openVideo(video)">댓글 보기</button>
            <template v-if="isAdmin">
              <span class="video-admin-actions" aria-label="관리자 영상 관리">
                <button class="text-action compact video-admin-action" type="button" @click.stop="openEditVideo(video)">수정하기</button>
                <button class="text-action compact danger video-admin-action" type="button" @click.stop="removeVideo(video)">삭제하기</button>
              </span>
            </template>
          </div>
        </article>
        <div v-if="videos.length === 0" class="recovery-panel empty">
          <strong>항목이 없어요.</strong>
          <p>{{ hasActiveFilter ? "검색어, 카테고리, 찜 필터를 바꾸면 다른 결과를 볼 수 있어요." : "관리자가 영상을 등록하면 이곳에 카드로 표시됩니다." }}</p>
          <div class="recovery-actions">
            <button class="primary" type="button" @click="retryLoad">다시 조회</button>
            <button v-if="hasActiveFilter" type="button" @click="resetFilters">조건 초기화</button>
          </div>
        </div>
      </div>

      <div v-if="!loading && !loadError && pageInfo.totalPages > 1" class="video-pagination case-pagination">
        <button type="button" :disabled="!canGoPrev" @click="changePage(pageInfo.page - 1)">이전</button>
        <button
          v-for="n in pageNumbers"
          :key="n"
          type="button"
          class="page-num"
          :class="{ current: n === pageInfo.page }"
          @click="changePage(n)"
        >
          {{ n + 1 }}
        </button>
        <button type="button" :disabled="!canGoNext" @click="changePage(pageInfo.page + 1)">다음</button>
      </div>
    </section>

    <div v-if="isReviewModalOpen" class="video-modal-backdrop" @click.self="closeReviewModal">
      <section class="video-modal" role="dialog" aria-modal="true">
        <header class="video-modal-head">
          <div>
            <span class="eyebrow">{{ selected.category }}</span>
            <h2>{{ selected.title }}</h2>
          </div>
          <button type="button" class="video-modal-close" @click="closeReviewModal">닫기</button>
        </header>

        <div class="video-modal-body">
          <aside class="video-modal-summary">
            <div class="video-modal-player">
              <iframe
                v-if="youtubeEmbedUrl(selected.youtubeUrl)"
                :src="youtubeEmbedUrl(selected.youtubeUrl)"
                :title="selected.title"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen
              ></iframe>
              <div v-else class="video-thumb-fallback">등록된 영상 주소를 확인해 주세요</div>
            </div>
            <button
              class="video-detail-bookmark"
              :class="{ saved: selected.bookmarked }"
              type="button"
              :aria-label="selected.bookmarked ? '찜 해제' : '찜하기'"
              @click="toggleSelectedBookmark"
            >
              <span class="heart-icon" aria-hidden="true"></span>
              <span>{{ selected.bookmarked ? "찜한 영상" : "찜하기" }}</span>
            </button>
            <dl>
              <div>
                <dt>증상</dt>
                <dd>{{ selected.symptom || "-" }}</dd>
              </div>
              <div>
                <dt>댓글</dt>
                <dd>{{ selected.reviewCount || reviews.length }}개</dd>
              </div>
              <div>
                <dt>찜</dt>
                <dd>{{ selected.bookmarkCount || 0 }}개</dd>
              </div>
            </dl>
            <p>{{ selected.description || "등록된 설명이 없습니다." }}</p>
          </aside>

          <section class="video-modal-comments">
            <div class="review-summary-strip">
              <span>댓글 {{ reviews.length }}개</span>
              <span>평균 평점 {{ Number(selected.averageRating || 0).toFixed(1) }}</span>
            </div>
            <form class="video-review-form" novalidate @submit.prevent="saveReview">
              <label>
                평점
                <div class="star-rating" role="radiogroup" aria-label="영상 평점">
                  <button
                    v-for="score in 5"
                    :key="score"
                    type="button"
                    :class="{ active: score <= reviewForm.rating }"
                    :aria-pressed="score === reviewForm.rating"
                    :aria-label="`${score}점`"
                    @click="reviewForm.rating = score"
                  >
                    ★
                  </button>
                </div>
              </label>
              <label>댓글<textarea v-model="reviewForm.content" rows="4" required /></label>
              <div class="form-actions">
                <button class="primary" type="submit">댓글 등록</button>
              </div>
            </form>

            <div class="comment-list">
              <article v-for="review in reviews" :key="`${review.id}-${reviewRating(review)}-${review.updatedAt || ''}`" class="comment-item">
                <div>
                  <strong class="comment-author">{{ review.userName }}</strong>
                  <div class="comment-meta-right">
                    <span class="review-stars" :aria-label="`${reviewRating(review)}점`">
                      <span v-for="score in 5" :key="score" :class="{ active: score <= reviewRating(review) }">★</span>
                    </span>
                  </div>
                </div>
                <form
                  v-if="editingReviewId === review.id"
                  class="inline-comment-edit"
                  novalidate
                  @submit.prevent="saveInlineReview(review)"
                >
                  <div class="star-rating compact" role="radiogroup" aria-label="댓글 평점 수정">
                    <button
                      v-for="score in 5"
                      :key="score"
                      type="button"
                      :class="{ active: score <= editingReviewForm.rating }"
                      :aria-pressed="score === editingReviewForm.rating"
                      :aria-label="`${score}점`"
                      @click="editingReviewForm.rating = score"
                    >
                      ★
                    </button>
                  </div>
                  <textarea v-model="editingReviewForm.content" rows="3" required />
                  <div class="form-actions">
                    <button type="button" @click="resetReview">취소</button>
                    <button class="primary" type="submit">수정 저장</button>
                  </div>
                </form>
                <p v-else>{{ review.content }}</p>
                <div v-if="editingReviewId !== review.id && (isMineReview(review) || canDeleteReview(review))" class="comment-actions">
                  <button v-if="isMineReview(review)" class="text-action compact" type="button" @click="editReview(review)">수정하기</button>
                  <button v-if="canDeleteReview(review)" class="text-action compact danger" type="button" @click="removeReview(review.id)">삭제하기</button>
                </div>
              </article>
              <div v-if="reviews.length === 0" class="recovery-panel empty comment-empty-panel">
                <strong>항목이 없어요.</strong>
                <p>첫 댓글을 남기면 이 영역에 표시됩니다.</p>
              </div>
            </div>
          </section>
        </div>
      </section>
    </div>

    <div v-if="isVideoFormOpen" class="video-modal-backdrop" @click.self="closeVideoForm">
      <section class="video-modal video-form-modal" role="dialog" aria-modal="true">
        <header class="video-modal-head">
          <div>
            <span class="eyebrow">Admin only</span>
            <h2>{{ videoFormTitle }}</h2>
          </div>
          <button type="button" class="video-modal-close" @click="closeVideoForm">닫기</button>
        </header>

        <form class="video-form-body" novalidate @submit.prevent="saveVideo">
          <div class="form-grid">
            <label><span class="form-label-text">제목 <span class="required-star">*</span></span><input v-model="form.title" required /></label>
            <label>
              <span class="form-label-text">카테고리 <span class="required-star">*</span></span>
              <CustomSelect v-model="form.category" :options="videoCategoryOptions" aria-label="카테고리" />
            </label>
            <label>증상<input v-model="form.symptom" /></label>
            <label><span class="form-label-text">YouTube URL <span class="required-star">*</span></span><input v-model="form.youtubeUrl" required /></label>
            <label class="wide">설명<textarea v-model="form.description" rows="4" /></label>
          </div>
          <div class="form-actions">
            <button type="button" @click="resetVideo">초기화</button>
            <button type="button" @click="closeVideoForm">취소</button>
            <button class="primary" type="submit">저장</button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
