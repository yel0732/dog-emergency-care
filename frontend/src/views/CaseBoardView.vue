<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { caseBoardApi } from "../api/resources";
import { userApi } from "../api/resources";
import { useAuthStore } from "../stores/auth";
import CustomSelect from "../components/CustomSelect.vue";
import { useConfirm } from "../composables/useConfirm";
import { CASE_SORT_OPTIONS, optionValues } from "../constants/queryOptions";
import { createMutationResync } from "../utils/stateRecovery";
import { formatNotice } from "../utils/messageFormat";

const sortOptions = CASE_SORT_OPTIONS;
const PAGE_SIZE = 9;

const route = useRoute();
const router = useRouter();

const auth = useAuthStore();
const confirm = useConfirm();
const posts = ref([]);
const comments = ref([]);
const selected = ref(null);
const revealedFollowPostId = ref(null);
const revealedCommentAuthorId = ref(null);
const detailFollowRevealed = ref(false);
const isPostModalOpen = ref(false);
const editingId = ref(null);
const editingCommentId = ref(null);
const error = ref("");
const loading = ref(false);
const loadError = ref("");
const detailError = ref("");
const pageInfo = reactive({ total: 0, page: 0, size: PAGE_SIZE, totalPages: 0 });
const filters = reactive({ category: "", keyword: "", authorId: "", authorName: "", sort: "latest", direction: "desc" });
const caseCategories = ["호흡/체온", "소화기", "신경", "외상/출혈", "중독", "피부/눈", "심장/순환", "기타"];
const categoryOptions = computed(() => [
  { value: "", label: "전체 카테고리" },
  ...caseCategories.map((c) => ({ value: c, label: c })),
]);
const postCategoryOptions = caseCategories.map((c) => ({ value: c, label: c }));
const postForm = reactive({ title: "", category: "호흡/체온", content: "" });
const postImages = ref([]); // data URL 또는 기존 이미지 주소 배열
const commentForm = reactive({ content: "" });
const editingCommentForm = reactive({ content: "" });
const replyTarget = ref(null);

const postModalTitle = computed(() => (editingId.value ? "게시글 수정" : "게시글 작성"));
const canGoPrev = computed(() => pageInfo.page > 0);
const canGoNext = computed(() => pageInfo.page + 1 < pageInfo.totalPages);
const hasActiveFilter = computed(() => Boolean(filters.category || filters.keyword || filters.authorId || filters.sort !== "latest"));
const caseListTitle = computed(() => (filters.authorId ? "작성자 게시글" : "사례 목록"));
const hasDetailRoute = computed(() => Boolean(route.params.id));
const rootComments = computed(() => comments.value.filter((comment) => !comment.parentId));
const isAdmin = computed(() => auth.me?.role === "ADMIN");
const pageNumbers = computed(() => {
  const total = pageInfo.totalPages;
  if (total <= 1) return [];
  const current = pageInfo.page;
  const start = Math.max(0, Math.min(current - 2, total - 5));
  const end = Math.min(total, start + 5);
  const arr = [];
  for (let i = start; i < end; i += 1) arr.push(i);
  return arr;
});

const sortValues = optionValues(sortOptions);
let syncingQuery = false;

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

const recoverCaseMutation = createMutationResync({
  reload: () => load(pageInfo.page, { syncUrl: false }),
  refreshDetail: async (detailId) => {
    if (detailId) {
      selected.value = await caseBoardApi.find(detailId);
      comments.value = await caseBoardApi.comments(detailId);
    }
  },
  isDetailMissing: isNotFound,
  onDetailMissing: () => closeDetail(false),
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

function onImageUpload(event) {
  const files = Array.from(event.target.files || []);
  for (const file of files) {
    if (!file.type.startsWith("image/")) {
      error.value = "이미지 파일만 업로드할 수 있습니다.";
      continue;
    }
    if (file.size > 1024 * 1024) {
      error.value = "이미지는 1MB 이하만 업로드할 수 있습니다.";
      continue;
    }
    const reader = new FileReader();
    reader.onload = () => postImages.value.push(String(reader.result || ""));
    reader.readAsDataURL(file);
  }
  event.target.value = "";
}

function removeImage(index) {
  postImages.value.splice(index, 1);
}

const categoryEmojiMap = {
  "호흡/체온": "🌡️",
  소화기: "🤢",
  신경: "⚡",
  "외상/출혈": "🩹",
  중독: "☠️",
  "피부/눈": "👁️",
  "심장/순환": "❤️",
  기타: "🐾",
};
function categoryEmoji(name) {
  return categoryEmojiMap[name] || "🐶";
}

// 업로드 이미지가 없을 때도 게시판 카드가 비어 보이지 않도록 실제 강아지 사진을 고정 fallback으로 사용합니다.
const dogPhotoFallbacks = Array.from({ length: 20 }, (_, index) => `/case-dogs/case-${String(index + 1).padStart(2, "0")}.jpg`);
function dogPhoto(id) {
  const n = Number(id) || 0;
  return dogPhotoFallbacks[Math.max(0, n - 1) % dogPhotoFallbacks.length];
}
function caseCardImage(post) {
  const primary = Array.isArray(post.imageUrls) ? post.imageUrls[0] : "";
  if (primary && !primary.includes("/case-dogs/case-") && !primary.endsWith(".svg")) return primary;
  return dogPhoto(post.id);
}
function caseDetailImages(post) {
  const images = Array.isArray(post?.imageUrls) ? post.imageUrls : [];
  const customImages = images.filter((image) => image && !image.includes("/case-dogs/case-") && !image.endsWith(".svg"));
  return customImages.length ? customImages : [dogPhoto(post?.id)];
}

function isReplyComment(comment) {
  return Boolean(comment?.parentId);
}

function repliesFor(comment) {
  return comments.value.filter((item) => item.parentId === comment.id);
}

function commentDisplayContent(comment) {
  const content = String(comment?.content || "").trim();
  return isReplyComment(comment) ? content.replace(/^@\S+\s+/, "").trim() : content;
}

function isMine(item) {
  return item?.userId === auth.me?.id;
}

function canDelete(item) {
  return isAdmin.value || isMine(item);
}

function formatDate(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function avatarSrc(value) {
  return value || "/choco-profile.png";
}

function setAvatarFallback(event) {
  event.target.src = "/choco-profile.png";
}

function resetPostForm() {
  editingId.value = null;
  Object.assign(postForm, { title: "", category: "호흡/체온", content: "" });
  postImages.value = [];
}

function openCreatePost() {
  error.value = "";
  resetPostForm();
  isPostModalOpen.value = true;
}

function openEditPost(post) {
  error.value = "";
  editingId.value = post.id;
  Object.assign(postForm, {
    title: post.title,
    category: post.category,
    content: post.content,
  });
  postImages.value = Array.isArray(post.imageUrls) ? [...post.imageUrls] : [];
  isPostModalOpen.value = true;
}

function closePostModal() {
  isPostModalOpen.value = false;
  resetPostForm();
}

function buildParams(page = pageInfo.page) {
  return {
    category: filters.category,
    keyword: filters.keyword,
    authorId: filters.authorId,
    sort: filters.sort,
    direction: filters.direction,
    page,
    size: PAGE_SIZE,
  };
}

function buildQuery(page) {
  const query = {};
  if (filters.keyword) query.keyword = filters.keyword;
  if (filters.category) query.category = filters.category;
  if (filters.authorId) query.authorId = filters.authorId;
  if (filters.authorName) query.authorName = filters.authorName;
  if (filters.sort !== "latest") query.sort = filters.sort;
  if (filters.direction !== "desc") query.direction = filters.direction;
  if (page > 0) query.page = String(page);
  return query;
}

function applyPage(response) {
  posts.value = response.items || [];
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
  filters.keyword = safeString(q.keyword);
  filters.category = safeString(q.category);
  filters.authorId = safeString(q.authorId);
  filters.authorName = safeString(q.authorName);
  filters.sort = safeOption(q.sort, sortValues, "latest");
  filters.direction = "desc";
  return safePage(q.page);
}

async function load(page = 0, options = {}) {
  error.value = "";
  loadError.value = "";
  if (!hasDetailRoute.value) detailError.value = "";
  loading.value = true;
  const requestedPage = safePage(page);
  try {
    let params = buildParams(requestedPage);
    let response = await caseBoardApi.list(params);
    if ((response.items || []).length === 0 && requestedPage > 0 && response.totalPages > 0) {
      params = buildParams(response.totalPages - 1);
      response = await caseBoardApi.list(params);
    }
    applyPage(response);
    if (options.syncUrl !== false) syncQueryToUrl(pageInfo.page);
  } catch (e) {
    loadError.value = "응급 사례 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
  } finally {
    loading.value = false;
  }
}

function search() {
  load(0);
}

function searchFromSubmit() {
  error.value = "";
  if (!filters.keyword.trim() && !filters.category) {
    error.value = "검색어를 입력하거나 카테고리를 선택해 주세요.";
    return;
  }
  search();
}

function resetFilters() {
  Object.assign(filters, {
    category: "",
    keyword: "",
    authorId: "",
    authorName: "",
    sort: "latest",
    direction: "desc",
  });
  load(0);
}

function retryLoad() {
  load(pageInfo.page, { syncUrl: false });
}

function changePage(nextPage) {
  if (nextPage < 0 || nextPage >= pageInfo.totalPages) return;
  load(nextPage);
}

function goDetail(id) {
  if (String(route.params.id || "") === String(id)) {
    openDetail(id);
    return;
  }
  router.push({ path: `/cases/${id}`, query: route.query }).catch(() => {});
}

async function savePost() {
  error.value = "";
  if (!postForm.title.trim() || !postForm.category.trim() || !postForm.content.trim()) {
    error.value = "제목, 카테고리, 내용을 모두 입력해 주세요.";
    return;
  }
  const body = {
    title: postForm.title.trim(),
    category: postForm.category.trim(),
    content: postForm.content.trim(),
    imageUrls: [...postImages.value],
  };
  try {
    const isEditing = Boolean(editingId.value);
    const saved = isEditing ? await caseBoardApi.update(editingId.value, body) : await caseBoardApi.create(body);
    closePostModal();
    await load(pageInfo.page);
    if (isEditing) {
      selected.value = saved;
    }
  } catch (e) {
    error.value = e.message;
  }
}

async function removePost(post) {
  const ok = await confirm.ask({
    title: "게시글 삭제",
    message: "게시글과 연결된 댓글이 함께 삭제됩니다. 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  error.value = "";
  try {
    await caseBoardApi.remove(post.id);
    selected.value = null;
    comments.value = [];
    // 페이지의 마지막 항목을 지웠다면 이전 페이지로 복귀
    const lastOnPage = posts.value.length <= 1 && pageInfo.page > 0;
    await load(lastOnPage ? pageInfo.page - 1 : pageInfo.page);
  } catch (e) {
    error.value = e.message;
  }
}

async function openDetail(id) {
  error.value = "";
  detailError.value = "";
  detailFollowRevealed.value = false;
  try {
    const detail = await caseBoardApi.find(id);
    if (!detail || !detail.id) {
      closeDetail(false);
      detailError.value = "게시글을 찾을 수 없습니다. 목록을 다시 불러왔습니다.";
      await load(pageInfo.page);
      return;
    }
    selected.value = detail;
    comments.value = (await caseBoardApi.comments(id)) || [];
    resetCommentForm();
    await load(pageInfo.page);
  } catch (e) {
    if (isNotFound(e)) {
      closeDetail(false);
      detailError.value = "게시글을 찾을 수 없습니다. 목록을 다시 불러왔습니다.";
    } else {
      detailError.value = "게시글 상세 정보를 불러오지 못했어요. 목록을 다시 불러온 뒤 다시 시도해 주세요.";
    }
    await load(pageInfo.page);
  }
}

function closeDetail(syncRoute = true) {
  selected.value = null;
  comments.value = [];
  detailError.value = "";
  detailFollowRevealed.value = false;
  resetCommentForm();
  if (syncRoute && route.params.id) {
    router.push({ path: "/cases", query: route.query }).catch(() => {});
  }
}

async function retryRouteDetail() {
  const id = Number(route.params.id);
  if (Number.isInteger(id) && id > 0) {
    await openDetail(id);
  } else {
    await load(pageInfo.page);
  }
}

function goCaseList() {
  detailError.value = "";
  router.push({ path: "/cases", query: route.query }).catch(() => {});
}

function canFollowAuthor(post) {
  return Boolean(post?.userId && post.userId !== auth.me?.id);
}

function isFollowButtonVisible(post) {
  return canFollowAuthor(post) && revealedFollowPostId.value === post.id;
}

function toggleFollowButton(post) {
  if (!canFollowAuthor(post)) return;
  revealedFollowPostId.value = revealedFollowPostId.value === post.id ? null : post.id;
}

function toggleDetailFollowButton() {
  if (!canFollowAuthor(selected.value)) return;
  detailFollowRevealed.value = !detailFollowRevealed.value;
}

async function toggleAuthorFollow(post) {
  if (!post || post.userId === auth.me?.id) return;
  error.value = "";
  try {
    const status = post.followingAuthor ? await userApi.unfollow(post.userId) : await userApi.follow(post.userId);
    const followingAuthor = status.following;
    const followerCount = status.followerCount;
    posts.value = posts.value.map((item) =>
      item.userId === post.userId ? { ...item, followingAuthor, followerCount } : item
    );
    if (selected.value?.userId === post.userId) {
      selected.value = { ...selected.value, followingAuthor, followerCount };
    }
  } catch (e) {
    error.value = e.message;
    await recoverCaseMutation(error.value, selected.value?.id);
  }
}

async function toggleSelectedAuthorFollow() {
  if (!selected.value) return;
  await toggleAuthorFollow(selected.value);
}

function canFollowCommentAuthor(comment) {
  return Boolean(comment?.userId && comment.userId !== auth.me?.id);
}

function isCommentFollowButtonVisible(comment) {
  return canFollowCommentAuthor(comment) && revealedCommentAuthorId.value === comment.id;
}

function toggleCommentFollowButton(comment) {
  if (!canFollowCommentAuthor(comment)) return;
  revealedCommentAuthorId.value = revealedCommentAuthorId.value === comment.id ? null : comment.id;
}

async function toggleCommentAuthorFollow(comment) {
  if (!canFollowCommentAuthor(comment)) return;
  error.value = "";
  try {
    const status = comment.followingAuthor ? await userApi.unfollow(comment.userId) : await userApi.follow(comment.userId);
    const followingAuthor = status.following;
    comments.value = comments.value.map((item) =>
      item.userId === comment.userId ? { ...item, followingAuthor } : item
    );
    posts.value = posts.value.map((item) =>
      item.userId === comment.userId ? { ...item, followingAuthor, followerCount: status.followerCount } : item
    );
    if (selected.value?.userId === comment.userId) {
      selected.value = { ...selected.value, followingAuthor, followerCount: status.followerCount };
    }
  } catch (e) {
    error.value = e.message;
    await recoverCaseMutation(error.value, selected.value?.id);
  }
}

function resetCommentForm() {
  editingCommentId.value = null;
  replyTarget.value = null;
  commentForm.content = "";
  editingCommentForm.content = "";
}

function editComment(comment) {
  error.value = "";
  editingCommentId.value = comment.id;
  replyTarget.value = null;
  editingCommentForm.content = commentDisplayContent(comment);
}

function replyToComment(comment) {
  error.value = "";
  editingCommentId.value = null;
  replyTarget.value = comment;
  commentForm.content = "";
}

async function saveComment() {
  if (!selected.value) return;
  error.value = "";
  if (!commentForm.content.trim()) {
    error.value = "댓글을 입력해 주세요.";
    return;
  }
  const body = { content: commentForm.content.trim(), parentId: replyTarget.value?.id || null };
  try {
    await caseBoardApi.createComment(selected.value.id, body);
    comments.value = await caseBoardApi.comments(selected.value.id);
    selected.value = await caseBoardApi.find(selected.value.id);
    resetCommentForm();
    await load(pageInfo.page);
  } catch (e) {
    if (isNotFound(e)) {
      closeDetail();
      error.value = "댓글 대상 게시글을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
    await recoverCaseMutation(error.value, selected.value?.id);
  }
}

async function saveInlineComment(comment) {
  if (!selected.value || editingCommentId.value !== comment.id) return;
  error.value = "";
  if (!editingCommentForm.content.trim()) {
    error.value = "댓글을 입력해 주세요.";
    return;
  }
  const body = { content: editingCommentForm.content.trim(), parentId: comment.parentId || null };
  try {
    await caseBoardApi.updateComment(comment.id, body);
    comments.value = await caseBoardApi.comments(selected.value.id);
    selected.value = await caseBoardApi.find(selected.value.id);
    resetCommentForm();
    await load(pageInfo.page);
  } catch (e) {
    if (isNotFound(e)) {
      closeDetail();
      error.value = "댓글 대상 게시글을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
    await recoverCaseMutation(error.value, selected.value?.id);
  }
}

async function removeComment(comment) {
  const ok = await confirm.ask({
    title: "댓글 삭제",
    message: "삭제한 댓글은 복구할 수 없습니다. 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  error.value = "";
  try {
    await caseBoardApi.removeComment(comment.id);
    comments.value = await caseBoardApi.comments(selected.value.id);
    selected.value = await caseBoardApi.find(selected.value.id);
    await load(pageInfo.page);
  } catch (e) {
    if (isNotFound(e)) {
      closeDetail();
      error.value = "댓글 대상 게시글을 찾을 수 없습니다.";
      await load(pageInfo.page);
      return;
    }
    error.value = e.message;
    await recoverCaseMutation(error.value, selected.value?.id);
  }
}

async function openRouteDetail() {
  const id = Number(route.params.id);
  if (Number.isInteger(id) && id > 0) {
    await openDetail(id);
  }
}

onMounted(async () => {
  const startPage = applyQueryFromUrl();
  await load(startPage);
  await openRouteDetail();
});

watch(
  () => route.query,
  () => {
    if (syncingQuery) return;
    const startPage = applyQueryFromUrl();
    load(startPage);
  }
);

watch(
  () => route.params.id,
  async (id) => {
    if (id) {
      await openRouteDetail();
    } else if (selected.value) {
      closeDetail(false);
    }
  }
);
</script>

<template>
  <main class="workspace case-board-workspace">
    <section class="case-board-hero">
      <div>
        <span class="eyebrow">Emergency community</span>
        <h1>응급 사례 게시판</h1>
        <p>보호자들이 겪은 응급 상황과 대처 경험을 공유하고, 댓글로 함께 확인합니다.</p>
      </div>
      <button class="primary" type="button" @click="openCreatePost">글쓰기</button>
    </section>

    <p v-if="error" class="message error">{{ formatNotice(error) }}</p>

    <section class="case-board-panel">
      <div class="case-board-toolbar">
        <div>
          <span class="eyebrow">Case board</span>
          <h2>{{ caseListTitle }}</h2>
        </div>
        <form class="case-board-filters" novalidate @submit.prevent="searchFromSubmit">
          <div class="filter-search">
            <span class="filter-search-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></svg>
            </span>
            <input v-model="filters.keyword" placeholder="제목·내용 검색" />
            <button class="filter-search-btn primary" type="submit">검색</button>
          </div>
          <div class="filter-options">
            <CustomSelect v-model="filters.category" :options="categoryOptions" aria-label="카테고리" @change="search" />
            <CustomSelect v-model="filters.sort" :options="sortOptions" aria-label="정렬 기준" @change="search" />
          </div>
        </form>
      </div>

      <div class="case-list-summary">
        <span>총 {{ pageInfo.total }}개</span>
        <span>{{ pageInfo.totalPages ? pageInfo.page + 1 : 0 }} / {{ pageInfo.totalPages }} 페이지</span>
      </div>

      <div v-if="hasDetailRoute && detailError" class="recovery-panel error">
        <strong>게시글 상세를 열지 못했습니다.</strong>
        <p>{{ detailError }}</p>
        <div class="recovery-actions">
          <button class="primary" type="button" @click="retryRouteDetail">다시 시도</button>
          <button type="button" @click="goCaseList">목록 주소로 돌아가기</button>
        </div>
      </div>

      <div v-if="loading" class="recovery-panel loading">
        <strong>응급 사례 목록을 불러오는 중입니다.</strong>
        <p>검색 조건과 정렬 상태를 기준으로 게시글을 확인하고 있어요.</p>
      </div>

      <div v-else-if="loadError" class="recovery-panel error">
        <strong>응급 사례 목록을 불러오지 못했어요.</strong>
        <p>{{ loadError }}</p>
        <div class="recovery-actions">
          <button class="primary" type="button" @click="retryLoad">다시 시도</button>
          <button type="button" @click="resetFilters">조건 초기화</button>
        </div>
      </div>

      <div v-else class="case-card-grid">
        <article v-for="post in posts" :key="post.id" class="case-card" @click="goDetail(post.id)">
          <div class="case-card-thumb">
            <div class="case-card-sick">
              <img :src="caseCardImage(post)" :alt="post.title || '강아지 사진'" loading="lazy" />
              <span class="sick-overlay"></span>
            </div>
            <span class="status-pill case-card-cat">{{ post.category }}</span>
            <span v-if="post.imageUrls && post.imageUrls.length > 1" class="case-card-count">＋{{ post.imageUrls.length - 1 }}</span>
          </div>

          <div class="case-card-body">
            <h3 class="case-card-title">{{ post.title }}</h3>

            <div class="case-card-foot">
              <div class="author-wrap">
                <button
                  v-if="canFollowAuthor(post)"
                  class="author-chip"
                  :class="{ clickable: true }"
                  type="button"
                  :aria-expanded="isFollowButtonVisible(post)"
                  @click.stop="toggleFollowButton(post)"
                >
                  <img :src="avatarSrc(post.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                  {{ post.userName }}
                </button>
                <span v-else class="author-chip">
                  <img :src="avatarSrc(post.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                  {{ post.userName }}
                </span>
                <button
                  v-if="isFollowButtonVisible(post)"
                  class="reveal-follow-btn"
                  :class="{ following: post.followingAuthor }"
                  type="button"
                  @click.stop="toggleAuthorFollow(post)"
                >
                  {{ post.followingAuthor ? "팔로우 취소" : "팔로우" }}
                </button>
                <span v-else-if="post.userId === auth.me?.id" class="author-self-badge">작성자</span>
              </div>
              <span class="case-card-date">{{ formatDate(post.createdAt) }}</span>
            </div>

            <div class="case-card-stats">
              <span><strong>{{ post.viewCount || 0 }}</strong> 조회</span>
              <span><strong>{{ post.commentCount || 0 }}</strong> 댓글</span>
            </div>
          </div>
        </article>
        <div v-if="posts.length === 0" class="recovery-panel empty">
          <strong>항목이 없어요.</strong>
          <p>{{ hasActiveFilter ? "검색어, 카테고리, 정렬 조건을 바꿔 다시 조회해 보세요." : "첫 응급 사례를 작성하면 이곳에 카드로 표시됩니다." }}</p>
          <div class="recovery-actions">
            <button class="primary" type="button" @click="retryLoad">다시 조회</button>
            <button v-if="hasActiveFilter" type="button" @click="resetFilters">조건 초기화</button>
          </div>
        </div>
      </div>

      <div v-if="!loading && !loadError && pageInfo.totalPages > 1" class="case-pagination">
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

    <div v-if="isPostModalOpen" class="case-modal-backdrop" @click.self="closePostModal">
      <section class="case-modal case-form-modal" role="dialog" aria-modal="true">
        <header class="case-modal-head">
          <div>
            <span class="eyebrow">Case editor</span>
            <h2>{{ postModalTitle }}</h2>
          </div>
          <button type="button" @click="closePostModal">닫기</button>
        </header>
        <form class="case-form-body" novalidate @submit.prevent="savePost">
          <p v-if="error" class="message error">{{ formatNotice(error) }}</p>
          <div class="form-grid">
            <label>제목<input v-model="postForm.title" required maxlength="300" placeholder="예: 경련 발생 시 영상 기록했던 경험" /></label>
            <label>카테고리
              <CustomSelect v-model="postForm.category" :options="postCategoryOptions" aria-label="카테고리" />
            </label>
            <div class="wide image-upload-field">
              <span class="upload-label">사진 첨부</span>
              <div class="image-upload-grid">
                <div v-for="(img, i) in postImages" :key="i" class="image-thumb">
                  <img :src="img" alt="첨부 이미지" />
                  <button type="button" class="image-thumb-remove" aria-label="삭제" @click="removeImage(i)">×</button>
                </div>
                <label class="image-upload-add">
                  <input type="file" accept="image/*" multiple @change="onImageUpload" />
                  <span class="plus">＋</span>
                  <span class="txt">사진 추가</span>
                </label>
              </div>
              <small class="upload-hint">이미지당 1MB 이하 · 여러 장 선택 가능</small>
            </div>
            <label class="wide">내용<textarea v-model="postForm.content" rows="8" required maxlength="5000" placeholder="상황, 증상, 대처, 병원에서 들은 내용을 적어주세요" /></label>
          </div>
          <div class="form-actions">
            <button type="button" @click="resetPostForm">초기화</button>
            <button class="primary" type="submit">저장</button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="selected" class="case-modal-backdrop" @click.self="closeDetail">
      <section class="case-modal case-detail-modal" role="dialog" aria-modal="true">
        <header class="case-modal-head">
          <div>
            <span class="eyebrow">{{ selected.category }}</span>
            <h2>{{ selected.title }}</h2>
          </div>
          <button type="button" @click="closeDetail">닫기</button>
        </header>

        <div class="case-detail-body">
          <article class="case-detail-content">
            <div class="case-detail-meta">
              <div class="author-wrap">
                <button
                  v-if="canFollowAuthor(selected)"
                  class="author-chip"
                  :class="{ clickable: true }"
                  type="button"
                  :aria-expanded="detailFollowRevealed"
                  @click="toggleDetailFollowButton"
                >
                  <img :src="avatarSrc(selected.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                  {{ selected.userName }}
                </button>
                <span v-else class="author-chip">
                  <img :src="avatarSrc(selected.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                  {{ selected.userName }}
                </span>
                <button
                  v-if="detailFollowRevealed && canFollowAuthor(selected)"
                  class="reveal-follow-btn"
                  :class="{ following: selected.followingAuthor }"
                  type="button"
                  @click="toggleSelectedAuthorFollow()"
                >
                  {{ selected.followingAuthor ? "팔로우 취소" : "팔로우" }}
                </button>
                <span v-else-if="selected.userId === auth.me?.id" class="author-self-badge">작성자</span>
              </div>
              <span>{{ formatDate(selected.createdAt) }}</span>
              <span>조회 {{ selected.viewCount || 0 }}</span>
              <span>댓글 {{ selected.commentCount || 0 }}</span>
            </div>
            <div v-if="caseDetailImages(selected).length" class="case-image-grid">
              <img v-for="image in caseDetailImages(selected)" :key="image" :src="image" alt="응급 사례 사진" />
            </div>
            <p>{{ selected.content }}</p>
            <div v-if="isMine(selected) || canDelete(selected)" class="case-detail-actions">
              <button v-if="isMine(selected)" class="text-action" type="button" @click="openEditPost(selected)">수정하기</button>
              <button v-if="canDelete(selected)" class="text-action danger" type="button" @click="removePost(selected)">삭제하기</button>
            </div>
          </article>

          <section class="case-comments">
            <div v-if="comments.length === 0" class="recovery-panel empty comment-empty-panel">
              <strong>항목이 없어요.</strong>
                <p>아래 입력창에서 첫 댓글을 남길 수 있습니다. 답글 버튼으로 대댓글을 시작하고, 수정·삭제는 본인 댓글에만 표시됩니다.</p>
            </div>

            <form v-if="!replyTarget" class="case-comment-form" novalidate @submit.prevent="saveComment">
              <label>댓글<textarea v-model="commentForm.content" rows="3" required maxlength="1000" placeholder="경험을 짧게 남기거나 댓글의 답글 버튼으로 답글을 작성하세요." /></label>
              <div class="form-actions">
                <button class="primary" type="submit">댓글 등록</button>
              </div>
            </form>

            <div class="comment-list">
              <article v-for="comment in rootComments" :key="comment.id" class="comment-item" :class="{ reply: isReplyComment(comment) }">
                <div>
                  <div class="comment-author-wrap">
                    <button
                      v-if="canFollowCommentAuthor(comment)"
                      type="button"
                      class="comment-author clickable"
                      :aria-expanded="isCommentFollowButtonVisible(comment)"
                      @click="toggleCommentFollowButton(comment)"
                    >
                      <img :src="avatarSrc(comment.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                      {{ comment.userName }}
                    </button>
                    <strong v-else class="comment-author">
                      <img :src="avatarSrc(comment.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                      {{ comment.userName }}
                    </strong>
                    <button
                      v-if="isCommentFollowButtonVisible(comment)"
                      type="button"
                      class="comment-follow-toggle"
                      :class="{ following: comment.followingAuthor }"
                      @click="toggleCommentAuthorFollow(comment)"
                    >
                      {{ comment.followingAuthor ? "팔로우 취소" : "팔로우" }}
                    </button>
                  </div>
                  <div class="comment-meta-right">
                    <time>{{ formatDate(comment.createdAt) }}</time>
                  </div>
                </div>
                <form
                  v-if="editingCommentId === comment.id"
                  class="inline-comment-edit"
                  novalidate
                  @submit.prevent="saveInlineComment(comment)"
                >
                  <textarea v-model="editingCommentForm.content" rows="3" required maxlength="1000" />
                  <div class="form-actions">
                    <button type="button" @click="resetCommentForm">취소</button>
                    <button class="primary" type="submit">수정 저장</button>
                  </div>
                </form>
                <p v-else>{{ commentDisplayContent(comment) }}</p>
                <div v-if="editingCommentId !== comment.id" class="comment-actions">
                  <button v-if="!isReplyComment(comment)" class="text-action compact reply-action" type="button" @click="replyToComment(comment)">답글</button>
                  <template v-if="isMine(comment)">
                    <button class="text-action compact" type="button" @click="editComment(comment)">수정하기</button>
                  </template>
                  <button v-if="canDelete(comment)" class="text-action compact danger" type="button" @click="removeComment(comment)">삭제하기</button>
                </div>
                <form
                  v-if="replyTarget?.id === comment.id"
                  class="case-comment-form inline-reply-form"
                  novalidate
                  @submit.prevent="saveComment"
                >
                  <label>답글<textarea v-model="commentForm.content" rows="3" required maxlength="1000" placeholder="답글을 작성하세요." /></label>
                  <div class="form-actions">
                    <button class="danger reply-cancel-button" type="button" @click="resetCommentForm">취소하기</button>
                    <button class="primary" type="submit">답글 등록</button>
                  </div>
                </form>
                <div v-if="repliesFor(comment).length" class="comment-replies">
                  <article v-for="reply in repliesFor(comment)" :key="reply.id" class="comment-item reply">
                    <div>
                      <div class="comment-author-wrap">
                        <button
                          v-if="canFollowCommentAuthor(reply)"
                          type="button"
                          class="comment-author clickable"
                          :aria-expanded="isCommentFollowButtonVisible(reply)"
                          @click="toggleCommentFollowButton(reply)"
                        >
                          <img :src="avatarSrc(reply.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                          {{ reply.userName }}
                        </button>
                        <strong v-else class="comment-author">
                          <img :src="avatarSrc(reply.userProfileImageUrl)" alt="" @error="setAvatarFallback" />
                          {{ reply.userName }}
                        </strong>
                        <button
                          v-if="isCommentFollowButtonVisible(reply)"
                          type="button"
                          class="comment-follow-toggle"
                          :class="{ following: reply.followingAuthor }"
                          @click="toggleCommentAuthorFollow(reply)"
                        >
                          {{ reply.followingAuthor ? "팔로우 취소" : "팔로우" }}
                        </button>
                      </div>
                      <div class="comment-meta-right">
                        <time>{{ formatDate(reply.createdAt) }}</time>
                      </div>
                    </div>
                    <form
                      v-if="editingCommentId === reply.id"
                      class="inline-comment-edit"
                      novalidate
                      @submit.prevent="saveInlineComment(reply)"
                    >
                      <textarea v-model="editingCommentForm.content" rows="3" required maxlength="1000" />
                      <div class="form-actions">
                        <button type="button" @click="resetCommentForm">취소</button>
                        <button class="primary" type="submit">수정 저장</button>
                      </div>
                    </form>
                    <p v-else>{{ commentDisplayContent(reply) }}</p>
                    <div v-if="editingCommentId !== reply.id" class="comment-actions">
                      <template v-if="isMine(reply)">
                        <button class="text-action compact" type="button" @click="editComment(reply)">수정하기</button>
                      </template>
                      <button v-if="canDelete(reply)" class="text-action compact danger" type="button" @click="removeComment(reply)">삭제하기</button>
                    </div>
                  </article>
                </div>
              </article>
            </div>
          </section>
        </div>
      </section>
    </div>
  </main>
</template>
