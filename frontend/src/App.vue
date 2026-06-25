<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "./stores/auth";
import ConfirmDialog from "./components/ConfirmDialog.vue";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const menuOpen = ref(false);
const sessionPanelRef = ref(null);

const showNav = computed(() => route.path !== "/login");
const userName = computed(() => auth.me?.nickname || auth.me?.name || "사용자");
const userProfileImage = computed(() => auth.me?.profileImageUrl || "/choco-profile.png");

const links = [
  { to: "/", label: "홈" },
  { to: "/emergency", label: "응급 체크" },
  { to: "/reports", label: "리포트" },
  { to: "/hospitals", label: "병원·지도" },
  { to: "/videos", label: "응급 영상" },
  { to: "/cases", label: "응급 사례" },
  { to: "/food-safety", label: "음식 안전" },
  { to: "/records", label: "기록하기" },
];

function goMyPage() {
  menuOpen.value = false;
  router.push("/users");
}

function goLogin() {
  menuOpen.value = false;
  router.push({ path: "/login", query: { redirect: route.fullPath } });
}

function goJoin() {
  menuOpen.value = false;
  router.push({ path: "/login", query: { mode: "join", redirect: route.fullPath } });
}

async function logout() {
  menuOpen.value = false;
  await auth.logout();
  router.push("/");
}

function setAvatarFallback(event) {
  event.target.src = "/choco-profile.png";
}

function closeMenuOnOutsideClick(event) {
  if (!menuOpen.value) return;
  if (sessionPanelRef.value?.contains(event.target)) return;
  menuOpen.value = false;
}

onMounted(() => {
  document.addEventListener("pointerdown", closeMenuOnOutsideClick);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", closeMenuOnOutsideClick);
});
</script>

<template>
  <div class="app-shell">
    <header v-if="showNav" class="app-header">
      <RouterLink class="brand" to="/">
        <img class="brand-logo" src="/logo.png" alt="구해줘 멍즈" />
        <strong class="brand-name">구해줘 멍즈</strong>
      </RouterLink>
      <nav class="tabs" aria-label="주요 메뉴">
        <RouterLink v-for="link in links" :key="link.to" :to="link.to">{{ link.label }}</RouterLink>
      </nav>
      <div v-if="auth.isLoggedIn" ref="sessionPanelRef" class="session-panel">
        <button type="button" class="user-menu-button" :class="{ open: menuOpen }" @click="menuOpen = !menuOpen">
          <img class="user-menu-avatar" :src="userProfileImage" alt="" @error="setAvatarFallback" />
          <span class="user-label">
            <span class="user-name">{{ userName }}</span><span class="user-suffix">님</span>
          </span>
        </button>
        <div v-if="menuOpen" class="user-menu">
          <button type="button" @click="goMyPage">마이페이지</button>
          <button type="button" @click="logout">로그아웃</button>
        </div>
      </div>
      <div v-else class="session-panel guest-auth-actions">
        <button type="button" class="user-menu-button guest-login-button" @click="goLogin">
          <span class="user-label">
            <span class="user-name">로그인</span>
          </span>
        </button>
        <button type="button" class="user-menu-button guest-join-button" @click="goJoin">
          <span class="user-label">
            <span class="user-name">회원가입</span>
          </span>
        </button>
      </div>
    </header>
    <RouterView />
    <ConfirmDialog />
  </div>
</template>
