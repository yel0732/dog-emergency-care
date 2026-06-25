<script setup>
import { computed, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { userApi } from "../api/resources";
import { useAuthStore } from "../stores/auth";
import { formatNotice } from "../utils/messageFormat";
import AppToast from "../components/AppToast.vue";

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const mode = ref(route.query.mode === "join" ? "join" : "login");
const error = ref("");
const toast = reactive({ show: false, type: "success", message: "" });
const loginForm = reactive({ loginId: "", password: "" });
const joinForm = reactive({ loginId: "", password: "", passwordConfirm: "", name: "", nickname: "", email: "" });
const termsAgreement = reactive({ terms: false, privacy: false });
const availability = reactive({ loginId: null, email: null, checkingLoginId: false, checkingEmail: false });
const joinTouched = ref(false);
let toastTimer = null;

const passwordRules = computed(() => [
  { key: "length", label: "8자 이상", ok: joinForm.password.length >= 8 },
  { key: "letter", label: "영문 포함", ok: /[A-Za-z]/.test(joinForm.password) },
  { key: "number", label: "숫자 포함", ok: /\d/.test(joinForm.password) },
  { key: "special", label: "특수문자 포함", ok: /[^A-Za-z0-9]/.test(joinForm.password) },
  { key: "space", label: "공백 없음", ok: joinForm.password.length > 0 && !/\s/.test(joinForm.password) },
]);
const passwordReady = computed(() => passwordRules.value.every((rule) => rule.ok));
const passwordMatches = computed(() => joinForm.passwordConfirm.length > 0 && joinForm.password === joinForm.passwordConfirm);
const joinReady = computed(() => {
  return joinForm.loginId
    && joinForm.name
    && joinForm.nickname
    && joinForm.email
    && passwordReady.value
    && passwordMatches.value
    && availability.loginId === true
    && availability.email === true
    && termsAgreement.terms
    && termsAgreement.privacy;
});

watch(() => joinForm.loginId, () => {
  availability.loginId = null;
});

watch(() => joinForm.email, () => {
  availability.email = null;
});

watch(() => route.query.mode, (nextMode) => {
  mode.value = nextMode === "join" ? "join" : "login";
});

function availabilityErrorMessage(error) {
  if (!error?.response) {
    return "서버 연결이 잠시 불안정해요. 다시 한 번 중복 확인을 눌러 주세요.";
  }
  return error.message || "중복 확인을 완료하지 못했어요. 잠시 후 다시 시도해 주세요.";
}

function showToast(message, type = "success") {
  toast.message = message;
  toast.type = type;
  toast.show = true;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.show = false;
  }, 3200);
}

async function login() {
  error.value = "";
  if (!loginForm.loginId.trim() || !loginForm.password) {
    error.value = "아이디와 비밀번호를 모두 입력해 주세요.";
    return;
  }
  try {
    await auth.login(loginForm);
    router.push(typeof route.query.redirect === "string" ? route.query.redirect : "/");
  } catch (e) {
    error.value = e.message;
  }
}

async function join() {
  error.value = "";
  joinTouched.value = true;
  if (!joinReady.value) {
    error.value = "아이디·이메일 중복 확인, 비밀번호 확인, 필수 약관 동의를 완료해 주세요.";
    return;
  }
  try {
    await userApi.create({
      loginId: joinForm.loginId.trim(),
      password: joinForm.password,
      name: joinForm.name.trim(),
      nickname: joinForm.nickname.trim(),
      email: joinForm.email.trim(),
    });
    Object.assign(loginForm, { loginId: joinForm.loginId.trim(), password: "" });
    Object.assign(joinForm, { loginId: "", password: "", passwordConfirm: "", name: "", nickname: "", email: "" });
    Object.assign(termsAgreement, { terms: false, privacy: false });
    Object.assign(availability, { loginId: null, email: null, checkingLoginId: false, checkingEmail: false });
    joinTouched.value = false;
    mode.value = "login";
    showToast("회원가입이 완료되었습니다. 로그인해 주세요.", "success");
  } catch (e) {
    error.value = e.message;
  }
}

async function checkLoginId() {
  error.value = "";
  if (!/^[A-Za-z][A-Za-z0-9_]{3,19}$/.test(joinForm.loginId)) {
    availability.loginId = false;
    error.value = "아이디는 영문으로 시작하고 4~20자로 입력해 주세요.";
    return;
  }
  availability.checkingLoginId = true;
  try {
    const result = await userApi.availability({ loginId: joinForm.loginId.trim() });
    availability.loginId = Boolean(result.loginIdAvailable);
  } catch (e) {
    availability.loginId = false;
    error.value = availabilityErrorMessage(e);
  } finally {
    availability.checkingLoginId = false;
  }
}

async function checkEmail() {
  error.value = "";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(joinForm.email)) {
    availability.email = false;
    error.value = "올바른 이메일 형식으로 입력해 주세요.";
    return;
  }
  availability.checkingEmail = true;
  try {
    const result = await userApi.availability({ email: joinForm.email.trim() });
    availability.email = Boolean(result.emailAvailable);
  } catch (e) {
    availability.email = false;
    error.value = availabilityErrorMessage(e);
  } finally {
    availability.checkingEmail = false;
  }
}
</script>

<template>
  <main class="login-page">
    <AppToast
      :show="toast.show"
      :type="toast.type"
      :message="toast.message"
      @close="toast.show = false"
    />
    <section class="login-card">
      <!-- 브랜드 -->
      <div class="login-brand">
        <div class="login-logo-wrap">
          <img class="brand-logo" src="/logo.png?v=20260622" alt="구해줘 멍즈" />
        </div>
        <p class="login-tagline">AI 기반 반려견 응급증상 분석 및 응급도 판단 지원 서비스</p>
      </div>

      <!-- 탭 -->
      <div class="login-segmented">
        <button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">로그인</button>
        <button type="button" :class="{ active: mode === 'join' }" @click="mode = 'join'">회원가입</button>
      </div>

      <p v-if="error" class="message error">{{ formatNotice(error) }}</p>

      <!-- 로그인 폼 -->
      <form v-if="mode === 'login'" class="login-form" novalidate @submit.prevent="login">
        <label>
          아이디
          <div class="input-icon-wrap">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
            <input v-model.trim="loginForm.loginId" required autocomplete="username" placeholder="아이디를 입력하세요" />
          </div>
        </label>
        <label>
          비밀번호
          <div class="input-icon-wrap">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input v-model="loginForm.password" type="password" required autocomplete="current-password" placeholder="비밀번호를 입력하세요" />
          </div>
        </label>
        <button class="primary login-submit-btn" type="submit">로그인</button>
      </form>

      <!-- 회원가입 폼 -->
      <form v-else class="login-form" novalidate @submit.prevent="join">
        <label>
          아이디
          <div class="input-action-wrap">
            <div class="input-icon-wrap">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>
            <input v-model.trim="joinForm.loginId" required minlength="4" maxlength="20" pattern="[A-Za-z][A-Za-z0-9_]{3,19}" title="영문으로 시작하고 영문, 숫자, 밑줄만 사용할 수 있습니다." autocomplete="username" placeholder="영문으로 시작, 4~20자" />
            </div>
            <button type="button" class="mini-check-btn" :disabled="availability.checkingLoginId" @click="checkLoginId">
              {{ availability.checkingLoginId ? "확인중" : "중복 확인" }}
            </button>
          </div>
          <small v-if="availability.loginId === true" class="field-feedback ok">사용 가능한 아이디입니다.</small>
          <small v-else-if="availability.loginId === false" class="field-feedback bad">이미 사용 중이거나 형식이 맞지 않습니다.</small>
        </label>
        <label>
          비밀번호
          <div class="input-icon-wrap">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            <input v-model="joinForm.password" type="password" required minlength="8" maxlength="60" pattern="(?=\S+$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,60}" title="공백 없이 영문, 숫자, 특수문자를 각각 1개 이상 포함해 8자 이상으로 입력해 주세요." autocomplete="new-password" placeholder="영문+숫자+특수문자 포함 8자 이상" />
          </div>
          <div class="password-checklist">
            <span v-for="rule in passwordRules" :key="rule.key" :class="{ ok: rule.ok }">{{ rule.label }}</span>
          </div>
        </label>
        <label>
          비밀번호 확인
          <div class="input-icon-wrap">
            <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 7 9 18l-5-5"/></svg>
            <input v-model="joinForm.passwordConfirm" type="password" required autocomplete="new-password" placeholder="비밀번호를 한 번 더 입력" />
          </div>
          <small v-if="passwordMatches" class="field-feedback ok">비밀번호가 일치합니다.</small>
          <small v-else-if="joinForm.passwordConfirm || joinTouched" class="field-feedback bad">비밀번호가 일치하지 않습니다.</small>
        </label>
        <label>이름<input v-model.trim="joinForm.name" required minlength="2" maxlength="50" pattern="[가-힣a-zA-Z\s]{2,50}" title="한글 또는 영문으로 입력해 주세요." placeholder="실명 입력" /></label>
        <label>닉네임<input v-model.trim="joinForm.nickname" required minlength="2" maxlength="50" pattern="[가-힣a-zA-Z0-9_\s]{2,50}" title="한글, 영문, 숫자, 밑줄만 사용할 수 있습니다." placeholder="서비스에서 사용할 이름" /></label>
        <label>
          이메일
          <div class="input-action-wrap">
            <input v-model.trim="joinForm.email" type="email" required maxlength="255" autocomplete="email" placeholder="example@email.com" />
            <button type="button" class="mini-check-btn" :disabled="availability.checkingEmail" @click="checkEmail">
              {{ availability.checkingEmail ? "확인중" : "중복 확인" }}
            </button>
          </div>
          <small v-if="availability.email === true" class="field-feedback ok">사용 가능한 이메일입니다.</small>
          <small v-else-if="availability.email === false" class="field-feedback bad">이미 사용 중이거나 형식이 맞지 않습니다.</small>
        </label>
        <p class="form-hint">비밀번호는 공백 없이 영문·숫자·특수문자를 포함하고, 아이디·이름·닉네임과 다르게 입력하세요.</p>
        <div class="terms-agreement-card" aria-label="필수 약관 동의">
          <div class="terms-row">
            <label class="terms-check">
              <input v-model="termsAgreement.terms" type="checkbox" />
              <span>이용약관에 동의합니다. <em>(필수)</em></span>
            </label>
            <button class="terms-link" type="button">이용약관 보기</button>
          </div>
          <div class="terms-row">
            <label class="terms-check">
              <input v-model="termsAgreement.privacy" type="checkbox" />
              <span>개인정보 수집·이용에 동의합니다. <em>(필수)</em></span>
            </label>
            <button class="terms-link" type="button">개인정보처리방침 보기</button>
          </div>
        </div>
        <button class="primary login-submit-btn" type="submit" :disabled="!joinReady">가입하고 시작하기</button>
      </form>

    </section>
  </main>
</template>
