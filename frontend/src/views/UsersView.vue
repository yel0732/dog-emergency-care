<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { petApi, userApi, videoApi } from "../api/resources";
import { useAuthStore } from "../stores/auth";
import { useConfirm } from "../composables/useConfirm";
import { createMutationResync } from "../utils/stateRecovery";
import { formatNotice } from "../utils/messageFormat";

const auth = useAuthStore();
const confirm = useConfirm();
const route = useRoute();
const router = useRouter();

const tabs = [
  { key: "profile", label: "프로필", icon: "👤" },
  { key: "pets", label: "반려견 등록", icon: "🐾" },
  { key: "favorites", label: "찜한 영상", icon: "❤️" },
  { key: "follow", label: "팔로우", icon: "👥" },
];
const activeTab = ref("profile");

const message = ref("");
const error = ref("");
const form = reactive({ password: "", passwordConfirm: "", name: "", nickname: "", email: "", profileImageUrl: "" });
const selectedProfileFileName = ref("");
const profilePreview = computed(() => form.profileImageUrl || "/choco-profile.png");
const pets = ref([]);
const petLoading = ref(false);
const editingPetId = ref(null);
const petKeyword = ref("");
const petForm = reactive({ name: "", breed: "", age: "", weight: "", genderStatus: "MALE_INTACT", allergies: "", diseases: "" });
const genderStatusOptions = [
  { value: "MALE_INTACT", label: "남아" },
  { value: "FEMALE_INTACT", label: "여아" },
  { value: "MALE_NEUTERED", label: "남아 · 중성화" },
  { value: "FEMALE_NEUTERED", label: "여아 · 중성화" },
];

const favorites = ref([]);
const favLoading = ref(false);
const followers = ref([]);
const following = ref([]);
const followLoading = ref(false);
const followLoaded = ref(false);
const followListMode = ref("following");
const visibleFollowing = computed(() => (followListMode.value === "followers" ? [] : following.value));
const visibleFollowers = computed(() => (followListMode.value === "following" ? [] : followers.value));
const filteredPets = computed(() => {
  const keyword = petKeyword.value.trim().toLowerCase();
  if (!keyword) return pets.value;
  return pets.value.filter((pet) => {
    return [pet.name, pet.breed, genderStatusLabel(pet), pet.allergies, pet.diseases]
      .some((value) => String(value || "").toLowerCase().includes(keyword));
  });
});

function commonLoadError(target = "항목") {
  return `${target}을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.`;
}

const recoverFavoritesMutation = createMutationResync({
  reload: loadFavorites,
  setMessage: (nextMessage) => {
    error.value = nextMessage || error.value;
  },
});

const recoverFollowMutation = createMutationResync({
  reload: loadFollow,
  setMessage: (nextMessage) => {
    error.value = nextMessage || error.value;
  },
});

function selectTab(key) {
  activeTab.value = key;
  const path = key === "favorites" ? "/favorites" : "/users";
  router.replace({ path, query: key === "profile" ? {} : { tab: key } });
}

function tabFromRoute() {
  if (route.meta?.tab) return String(route.meta.tab);
  if (route.path === "/favorites") return "favorites";
  if (["/follow", "/followers", "/following"].includes(route.path)) return "follow";
  return String(route.query.tab || "profile");
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

function syncFollowModeFromRoute() {
  if (route.path === "/followers") followListMode.value = "followers";
  else if (route.path === "/following") followListMode.value = "following";
  else if (route.path === "/follow" || route.query.tab === "follow") followListMode.value = "following";
}

async function loadProfile() {
  error.value = "";
  try {
    await auth.loadMe();
    Object.assign(form, {
      password: "",
      passwordConfirm: "",
      name: auth.me?.name || "",
      nickname: auth.me?.nickname || auth.me?.name || "",
      email: auth.me?.email || "",
      profileImageUrl: auth.me?.profileImageUrl || "/choco-profile.png",
    });
  } catch (e) {
    error.value = commonLoadError("내 정보");
  }
}

async function saveMe() {
  message.value = "";
  error.value = "";
  if (!form.name.trim() || !form.nickname.trim() || !form.email.trim()) {
    error.value = "이름, 닉네임, 이메일을 모두 입력해 주세요.";
    return;
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    error.value = "올바른 이메일 형식으로 입력해 주세요.";
    return;
  }
  if (form.password && form.password !== form.passwordConfirm) {
    error.value = "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.";
    return;
  }
  try {
    auth.me = await userApi.updateMe({
      password: form.password,
      name: form.name.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      profileImageUrl: form.profileImageUrl.trim(),
    });
    message.value = "내 정보가 저장되었습니다.";
    await loadProfile();
  } catch (e) {
    error.value = e.message;
  }
}

function setAvatarFallback(event) {
  event.target.src = "/choco-profile.png";
}

function uploadProfileImage(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    error.value = "이미지 파일만 선택해 주세요.";
    event.target.value = "";
    return;
  }
  if (file.size > 900 * 1024) {
    error.value = "프로필 사진은 900KB 이하 이미지만 사용할 수 있습니다.";
    event.target.value = "";
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    form.profileImageUrl = String(reader.result || "");
    selectedProfileFileName.value = file.name;
    error.value = "";
  };
  reader.readAsDataURL(file);
}

function clearProfileImage() {
  form.profileImageUrl = "/choco-profile.png";
  selectedProfileFileName.value = "";
}

function resetPetForm() {
  editingPetId.value = null;
  Object.assign(petForm, { name: "", breed: "", age: "", weight: "", genderStatus: "MALE_INTACT", allergies: "", diseases: "" });
}

async function loadPets() {
  petLoading.value = true;
  error.value = "";
  try {
    pets.value = await petApi.list();
  } catch (e) {
    error.value = commonLoadError("반려견 목록");
    pets.value = [];
  } finally {
    petLoading.value = false;
  }
}

function editPet(pet) {
  editingPetId.value = pet.id;
  Object.assign(petForm, {
    name: pet.name || "",
    breed: pet.breed || "",
    age: pet.age ?? "",
    weight: pet.weight ?? "",
    genderStatus: toGenderStatus(pet.gender, pet.neutered),
    allergies: pet.allergies || "",
    diseases: pet.diseases || "",
  });
}

async function savePet() {
  message.value = "";
  error.value = "";
  if (!petForm.name.trim()) {
    error.value = "반려견 이름을 입력해 주세요.";
    return;
  }
  const genderPayload = fromGenderStatus(petForm.genderStatus);
  const body = {
    name: petForm.name.trim(),
    breed: petForm.breed || null,
    age: petForm.age === "" ? null : Number(petForm.age),
    weight: petForm.weight === "" ? null : Number(petForm.weight),
    gender: genderPayload.gender,
    neutered: genderPayload.neutered,
    allergies: petForm.allergies || null,
    diseases: petForm.diseases || null,
  };
  try {
    editingPetId.value ? await petApi.update(editingPetId.value, body) : await petApi.create(body);
    message.value = editingPetId.value ? "반려견 정보가 수정되었습니다." : "반려견이 등록되었습니다.";
    resetPetForm();
    await loadPets();
  } catch (e) {
    error.value = e.message || commonLoadError("반려견 저장");
  }
}

async function removePet(id) {
  const ok = await confirm.ask({
    title: "반려견 정보 삭제",
    message: "등록된 반려견 정보를 삭제할까요?",
    confirmText: "삭제",
  });
  if (!ok) return;
  try {
    await petApi.remove(id);
    await loadPets();
    if (editingPetId.value === id) resetPetForm();
  } catch (e) {
    error.value = e.message || commonLoadError("반려견 삭제");
  }
}

async function withdraw() {
  const ok = await confirm.ask({
    title: "회원 탈퇴",
    message: "계정과 연결된 정보가 비활성화됩니다. 정말 탈퇴하시겠습니까?",
    confirmText: "탈퇴",
  });
  if (!ok) return;
  await userApi.removeMe();
  location.href = "/login";
}

/* ---- 찜한 영상 ---- */
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
function youtubeThumbnailUrl(url) {
  const id = youtubeId(url);
  return id ? `https://img.youtube.com/vi/${id}/hqdefault.jpg` : "";
}

async function loadFavorites() {
  favLoading.value = true;
  error.value = "";
  try {
    const response = await videoApi.bookmarks({ size: 60, sort: "latest", direction: "desc" });
    favorites.value = response.items || [];
  } catch (e) {
    error.value = commonLoadError("찜한 영상");
  } finally {
    favLoading.value = false;
  }
}

async function removeFavorite(video) {
  error.value = "";
  try {
    await videoApi.unbookmark(video.id);
    favorites.value = favorites.value.filter((v) => v.id !== video.id);
  } catch (e) {
    error.value = e.message;
    await recoverFavoritesMutation(error.value);
  }
}

function goToVideos() {
  router.push("/videos");
}

function openFavorite(video) {
  router.push({ path: "/videos", query: { keyword: video.title || "" } });
}

/* ---- 팔로우 ---- */
async function loadFollow() {
  followLoading.value = true;
  error.value = "";
  try {
    // 팔로워/팔로잉 목록을 병렬로 가져와 화면 진입 시 왕복 시간을 줄인다.
    const [f1, f2] = await Promise.all([userApi.following(), userApi.followers()]);
    following.value = f1 || [];
    followers.value = f2 || [];
    followLoaded.value = true;
  } catch (e) {
    error.value = commonLoadError("팔로우 목록");
  } finally {
    followLoading.value = false;
  }
}

async function toggleFollow(person) {
  error.value = "";
  try {
    const status = person.following ? await userApi.unfollow(person.id) : await userApi.follow(person.id);
    const patch = { following: status.following, followerCount: status.followerCount };
    following.value = following.value.map((p) => (p.id === person.id ? { ...p, ...patch } : p));
    followers.value = followers.value.map((p) => (p.id === person.id ? { ...p, ...patch } : p));
  } catch (e) {
    error.value = e.message;
    await recoverFollowMutation(error.value);
  }
}

function viewPersonCases(person) {
  if (!person?.id) return;
  router.push({
    path: "/cases",
    query: {
      authorId: String(person.id),
      authorName: person.nickname || person.name || "",
    },
  });
}

function personFollowingCount(person) {
  return person.followingCount ?? person.followCount ?? 0;
}

function openFollowTab(mode) {
  followListMode.value = mode;
  selectTab("follow");
}

function ensureTabData(key) {
  if (key === "pets" && pets.value.length === 0) loadPets();
  if (key === "favorites" && favorites.value.length === 0) loadFavorites();
  if (key === "follow" && !followLoaded.value) loadFollow();
}

watch(activeTab, (key) => ensureTabData(key));
watch(
  () => [route.path, route.query.tab],
  () => {
    syncFollowModeFromRoute();
    const tab = tabFromRoute();
    if (tabs.some((t) => t.key === tab)) {
      activeTab.value = tab;
      ensureTabData(tab);
    }
  }
);

onMounted(async () => {
  await loadProfile();
  loadFollow();
  syncFollowModeFromRoute();
  const tab = tabFromRoute();
  if (tabs.some((t) => t.key === tab)) activeTab.value = tab;
  ensureTabData(activeTab.value);
});
</script>

<template>
  <main class="workspace mypage-layout">
    <section class="mypage-hero">
      <img class="mypage-hero-avatar" :src="profilePreview" alt="프로필" @error="setAvatarFallback" />
      <div class="mypage-hero-copy">
        <span class="eyebrow">My page</span>
        <h1>{{ form.nickname || form.name || "보호자" }}님</h1>
        <p>{{ form.email || "이메일 미등록" }}</p>
      </div>
      <div class="mypage-profile-stats" aria-label="팔로우 요약">
        <button type="button" @click="openFollowTab('followers')">
          <strong>{{ followers.length }}</strong>
          <span>팔로워</span>
        </button>
        <button type="button" @click="openFollowTab('following')">
          <strong>{{ following.length }}</strong>
          <span>팔로잉</span>
        </button>
      </div>
    </section>

    <nav class="mypage-tabs" aria-label="마이페이지 메뉴">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="mypage-tab"
        :class="{ active: activeTab === tab.key }"
        @click="selectTab(tab.key)"
      >
        <span class="tab-icon" aria-hidden="true">{{ tab.icon }}</span>{{ tab.label }}
      </button>
    </nav>

    <p v-if="message" class="message">{{ message }}</p>
    <p v-if="error" class="message error">{{ formatNotice(error) }}</p>

    <!-- ── 프로필 탭 ── -->
    <form v-if="activeTab === 'profile'" class="mypage-card profile-form" novalidate @submit.prevent="saveMe">
      <div class="mypage-card-head">
        <h2>프로필 수정</h2>
        <p>계정 정보와 프로필 사진을 관리하세요.</p>
      </div>

      <div class="profile-photo-editor">
        <div class="profile-photo-frame">
          <img class="profile-photo-preview" :src="profilePreview" alt="프로필 사진 미리보기" @error="setAvatarFallback" />
        </div>
        <div class="profile-photo-copy">
          <strong>프로필 사진</strong>
          <p>등록한 사진은 헤더와 게시판·댓글에 함께 표시됩니다. (900KB 이하 이미지)</p>
          <div class="profile-upload-row">
            <label class="profile-upload-button">
              사진 선택
              <input type="file" accept="image/*" @change="uploadProfileImage" />
            </label>
            <button type="button" class="ghost-btn" @click="clearProfileImage">기본 이미지</button>
            <span class="profile-file-name">{{ selectedProfileFileName || "선택된 사진 없음" }}</span>
          </div>
        </div>
      </div>

      <div class="profile-field-grid">
        <label>이름<input v-model.trim="form.name" required minlength="2" maxlength="50" pattern="[가-힣a-zA-Z\s]{2,50}" title="한글 또는 영문으로 입력해 주세요." /></label>
        <label>닉네임<input v-model.trim="form.nickname" required minlength="2" maxlength="50" pattern="[가-힣a-zA-Z0-9_\s]{2,50}" title="한글, 영문, 숫자, 밑줄만 사용할 수 있습니다." /></label>
        <label>이메일<input v-model.trim="form.email" type="email" required maxlength="255" /></label>
        <label class="wide">
          새 비밀번호
          <input
            v-model="form.password"
            type="password"
            maxlength="60"
            pattern="^$|(?=\S+$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,60}"
            title="공백 없이 영문, 숫자, 특수문자를 각각 1개 이상 포함해 8자 이상으로 입력해 주세요."
            autocomplete="new-password"
            placeholder="변경할 때만 입력 (영문·숫자·특수문자 포함 8자 이상)"
          />
        </label>
        <label class="wide">
          새 비밀번호 확인
          <input
            v-model="form.passwordConfirm"
            type="password"
            maxlength="60"
            autocomplete="new-password"
            placeholder="새 비밀번호를 한 번 더 입력"
          />
        </label>
      </div>

      <div class="mypage-actions">
        <button class="primary" type="submit">변경사항 저장</button>
        <button class="danger" type="button" @click="withdraw">회원 탈퇴</button>
      </div>
    </form>

    <!-- ── 찜한 영상 탭 ── -->
    <section v-if="activeTab === 'pets'" class="mypage-card mypage-pet-card">
      <section class="mypage-pet-entry-card">
        <div class="mypage-card-head">
          <div>
            <h2>반려견 등록</h2>
            <p>응급 체크와 기록 작성에 사용할 반려견 정보를 관리합니다.</p>
          </div>
        </div>

        <form class="pet-form-body mypage-pet-form" novalidate @submit.prevent="savePet">
          <div class="pet-form-row">
            <label><span class="lbl">이름 <span class="req">*</span></span><input v-model.trim="petForm.name" required placeholder="예: 초코" /></label>
            <label><span class="lbl">견종</span><input v-model.trim="petForm.breed" maxlength="60" placeholder="예: 푸들" /></label>
          </div>
          <div class="pet-form-row">
            <label><span class="lbl">나이 (세)</span><input v-model="petForm.age" type="number" min="0" max="40" placeholder="0" /></label>
            <label><span class="lbl">체중 (kg)</span><input v-model="petForm.weight" type="number" min="0.1" max="120" step="0.1" placeholder="0.0" /></label>
          </div>
          <div class="wide">
            <span class="lbl">성별 / 중성화 여부</span>
            <div class="segmented-choice-grid">
              <button
                v-for="option in genderStatusOptions"
                :key="option.value"
                type="button"
                class="segmented-choice"
                :class="{ active: petForm.genderStatus === option.value }"
                @click="petForm.genderStatus = option.value"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <label><span class="lbl">알레르기</span><input v-model.trim="petForm.allergies" maxlength="255" placeholder="없으면 비워두세요" /></label>
          <label><span class="lbl">기저질환</span><input v-model.trim="petForm.diseases" maxlength="255" placeholder="없으면 비워두세요" /></label>
          <div class="pet-form-actions">
            <button class="primary" type="submit">{{ editingPetId ? "수정 저장" : "등록하기" }}</button>
          </div>
        </form>
      </section>

      <section class="mypage-pet-list-panel">
        <div class="mypage-pet-list-head">
          <h2>등록된 반려견</h2>
          <div class="mypage-pet-list-tools">
            <input v-model.trim="petKeyword" type="search" placeholder="이름, 견종, 성별, 특이사항 검색" />
          </div>
        </div>

        <div v-if="petLoading" class="mypage-empty">불러오는 중...</div>
        <div v-else-if="filteredPets.length > 0" class="pet-card-grid mypage-pet-list">
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
              <button class="text-action compact" type="button" @click="editPet(pet)">수정하기</button>
              <button class="text-action compact danger" type="button" @click="removePet(pet.id)">삭제하기</button>
            </div>
          </article>
        </div>
        <div v-else class="mypage-empty">
          <strong>{{ pets.length ? "검색 결과가 없습니다." : "등록된 반려견이 없습니다." }}</strong>
          <span>{{ pets.length ? "검색어를 줄이거나 다른 단어로 찾아보세요." : "왼쪽 입력란에서 첫 반려견을 등록해 주세요." }}</span>
        </div>
      </section>
    </section>

    <section v-if="activeTab === 'favorites'" class="mypage-card">
      <div class="mypage-card-head">
        <div>
          <h2>찜한 영상 <span class="count-chip">{{ favorites.length }}</span></h2>
          <p>응급 영상 카드의 하트 버튼으로 담고, 이 화면에서 다시 확인하거나 해제할 수 있습니다.</p>
        </div>
        <button type="button" class="ghost-btn" @click="loadFavorites">새로고침</button>
      </div>

      <p v-if="favLoading" class="empty-state">불러오는 중…</p>
      <div v-else-if="favorites.length > 0" class="fav-grid">
        <article v-for="video in favorites" :key="video.id" class="fav-card">
          <button type="button" class="fav-thumb" @click="goToVideos">
            <img v-if="youtubeThumbnailUrl(video.youtubeUrl)" :src="youtubeThumbnailUrl(video.youtubeUrl)" :alt="video.title" />
            <span v-else class="fav-thumb-fallback">영상</span>
            <span class="fav-play" aria-hidden="true">▶</span>
          </button>
          <div class="fav-body">
            <span class="status-pill">{{ video.category }}</span>
            <strong>{{ video.title }}</strong>
            <div class="fav-metrics">
              <span>댓글 {{ video.reviewCount || 0 }}</span>
              <span>평균 {{ Number(video.averageRating || 0).toFixed(1) }}</span>
              <span>찜 {{ video.bookmarkCount || 0 }}</span>
            </div>
            <div class="fav-card-actions">
              <button type="button" class="ghost-btn" @click="openFavorite(video)">영상에서 보기</button>
              <button type="button" class="danger ghost-btn" @click="removeFavorite(video)">찜 해제</button>
            </div>
          </div>
          <button
            type="button"
            class="fav-remove"
            aria-label="찜 해제"
            title="찜 해제"
            @click="removeFavorite(video)"
          >
            <span class="heart-icon" aria-hidden="true"></span>
          </button>
        </article>
      </div>
      <div v-else class="mypage-empty">
        <span class="empty-emoji" aria-hidden="true">🤍</span>
        <p>항목이 없어요.<br />응급처치 영상에서 하트를 눌러 모아 보세요.</p>
        <button class="primary" type="button" @click="goToVideos">응급 영상 보러가기</button>
      </div>
    </section>

    <!-- ── 팔로우 탭 ── -->
    <section v-if="activeTab === 'follow'" class="mypage-card follow-card">
      <div class="mypage-card-head">
        <div>
          <h2>팔로우 관리</h2>
        </div>
      </div>

      <p v-if="followLoading" class="empty-state">불러오는 중…</p>
      <div v-else class="follow-columns">
        <div v-if="followListMode !== 'followers'" class="follow-col following-col">
          <div v-if="visibleFollowing.length > 0" class="follow-list">
            <article v-for="person in visibleFollowing" :key="'f' + person.id" class="follow-item">
              <img :src="person.profileImageUrl || '/choco-profile.png'" alt="" @error="setAvatarFallback" />
              <div class="follow-meta">
                <div class="follow-account-line">
                  <strong>{{ person.nickname }}</strong>
                  <span>팔로잉 {{ personFollowingCount(person) }}</span>
                  <span>팔로워 {{ person.followerCount || 0 }}</span>
                </div>
              </div>
              <div class="follow-item-actions">
                <button type="button" class="follow-case-link" @click="viewPersonCases(person)">게시글 보기</button>
                <button
                  type="button"
                  class="follow-toggle"
                  :class="{ following: person.following }"
                  @click="toggleFollow(person)"
                >
                  {{ person.following ? "팔로우 취소" : "팔로우" }}
                </button>
              </div>
            </article>
          </div>
          <div v-else class="follow-empty-action">
            <strong>항목이 없어요.</strong>
            <p>응급 사례 게시글에서 작성자 이름을 누르면 팔로우할 수 있습니다.</p>
            <button type="button" @click="router.push('/cases')">사례 게시판 보기</button>
          </div>
        </div>

        <div v-if="followListMode !== 'following'" class="follow-col followers-col">
          <div v-if="visibleFollowers.length > 0" class="follow-list">
            <article v-for="person in visibleFollowers" :key="'r' + person.id" class="follow-item">
              <img :src="person.profileImageUrl || '/choco-profile.png'" alt="" @error="setAvatarFallback" />
              <div class="follow-meta">
                <div class="follow-account-line">
                  <strong>{{ person.nickname }}</strong>
                  <span>팔로잉 {{ personFollowingCount(person) }}</span>
                  <span>팔로워 {{ person.followerCount || 0 }}</span>
                </div>
              </div>
              <button
                type="button"
                class="follow-toggle"
                :class="{ following: person.following }"
                @click="toggleFollow(person)"
              >
                {{ person.following ? "팔로우 취소" : "팔로우" }}
              </button>
            </article>
          </div>
          <div v-else class="follow-empty-action">
            <strong>항목이 없어요.</strong>
            <p>게시글과 댓글 활동이 쌓이면 보호자 관계를 확인할 수 있습니다.</p>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
