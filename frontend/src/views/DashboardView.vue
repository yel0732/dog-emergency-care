<script setup>
import { computed, onMounted, ref } from "vue";
import { emergencyApi } from "../api/resources";
import { useAuthStore } from "../stores/auth";

const auth = useAuthStore();
const checks = ref([]);

onMounted(async () => {
  if (!auth.isLoggedIn) {
    checks.value = [];
    return;
  }
  try {
    checks.value = await emergencyApi.list();
  } catch (e) {
    checks.value = [];
  }
});

const recent = computed(() => checks.value.slice(0, 5));

function riskClass(level) {
  const v = String(level || "");
  if (/위험|위급|응급|높|심각|emergency|danger|high/i.test(v)) return "urgent";
  if (/주의|중간|caution|medium/i.test(v)) return "caution";
  if (/관찰|낮|observe|watch|low/i.test(v)) return "observe";
  return "safe";
}

function petInitial(name) {
  return (name || "?").trim().charAt(0) || "?";
}
</script>

<template>
  <main class="workspace dashboard-layout">
    <!-- HERO -->
    <section class="home-hero">
      <div class="home-hero-main">
        <span class="hero-pulse"><span class="dot"></span>응급 체크 · 병원 정보 확인</span>
        <h1>증상만 입력하면,<br /><span class="text-gradient">응급도부터 대처까지</span> <span class="hero-nowrap">한 번에.</span></h1>
        <p>30초 안에 응급도를 확인하고, 병원 전달 리포트까지 준비하세요.</p>
        <div class="hero-cta-row">
          <RouterLink class="btn-red" to="/emergency">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
            응급 체크 시작
          </RouterLink>
          <RouterLink class="btn-line" to="/hospitals">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z"/><circle cx="12" cy="10" r="3"/></svg>
            가까운 병원 찾기
          </RouterLink>
        </div>
      </div>

      <aside class="hero-stage">
        <span class="stage-blob blob-x"></span>
        <span class="stage-blob blob-y"></span>
        <span class="stage-ring"></span>
        <div class="stage-disc"></div>
        <img class="stage-mascot" src="/logo.png" alt="구해줘 멍즈 마스코트" />

        <span class="ochip o1" style="--d:0s">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.27 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.77-3.4 6.86-8.55 11.53L12 21.35z"/></svg>
        </span>
        <span class="ochip o3" style="--d:1.5s">
          <svg viewBox="0 0 24 24" fill="currentColor"><circle cx="6" cy="10" r="2.1"/><circle cx="10.5" cy="6.5" r="2.1"/><circle cx="15.5" cy="6.5" r="2.1"/><circle cx="19" cy="10.5" r="2.1"/><path d="M12.5 12c2.6 0 4.6 2 4.6 4 0 1.7-1.4 2.5-3 2.5-1 0-1.1-.4-2.4-.4s-1.4.4-2.4.4c-1.6 0-3-.8-3-2.5 0-2 2-4 4.6-4z"/></svg>
        </span>
        <span class="ochip o4" style="--d:2.2s">
          <svg viewBox="0 0 24 24"><g transform="rotate(45 12 12)"><rect x="3.5" y="8.5" width="17" height="7" rx="3.5" fill="currentColor"/><g fill="#fff"><circle cx="9.5" cy="10.4" r="0.85"/><circle cx="12" cy="10.4" r="0.85"/><circle cx="14.5" cy="10.4" r="0.85"/><circle cx="9.5" cy="13.6" r="0.85"/><circle cx="12" cy="13.6" r="0.85"/><circle cx="14.5" cy="13.6" r="0.85"/></g></g></svg>
        </span>
        <span class="ochip o5 siren" style="--d:1.1s">
          <svg viewBox="0 0 24 24" fill="currentColor"><path d="M5 20h14v2H5z"/><path d="M7 19a5 5 0 0 1 10 0z"/><rect x="11" y="2.5" width="2" height="3" rx="1"/><path d="M3 10l1.7.9M21 10l-1.7.9" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/></svg>
        </span>
      </aside>
    </section>

    <!-- QUICK -->
    <div class="sec-head"><h2>빠른 케어</h2></div>
    <section class="quick-grid">
      <RouterLink class="quick-card em" to="/emergency">
        <div class="q-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg></div>
        <h3>응급 체크</h3>
        <p>증상 기반 응급도 판단과 즉시 대처 안내</p>
        <span class="go">지금 확인 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
      </RouterLink>
      <RouterLink class="quick-card" to="/hospitals">
        <div class="q-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0Z"/><path d="M12 7v6M9 10h6"/></svg></div>
        <h3>가까운 병원</h3>
        <p>응급·야간 동물병원과 길찾기</p>
        <span class="go">지도 열기 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
      </RouterLink>
      <RouterLink class="quick-card" to="/food-safety">
        <div class="q-ic">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M5 11h14l-1.4 6.2A3 3 0 0 1 14.7 19H9.3a3 3 0 0 1-2.9-1.8L5 11Z" />
            <path d="M7 11c.6-2.4 2.4-4 5-4s4.4 1.6 5 4" />
            <path d="M9 15.2l1.7 1.7L15.4 12" />
            <path d="M10 5.5h4" />
          </svg>
        </div>
        <h3>음식 안전</h3>
        <p>위험 음식과 즉시 진료 여부 검색</p>
        <span class="go">검색하기 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
      </RouterLink>
      <RouterLink class="quick-card" to="/videos">
        <div class="q-ic"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="3"/><path d="m10 9 5 3-5 3V9Z"/></svg></div>
        <h3>응급처치 영상</h3>
        <p>상황별 응급처치 가이드 영상</p>
        <span class="go">영상 보기 <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14M13 6l6 6-6 6"/></svg></span>
      </RouterLink>
    </section>

    <!-- LOWER -->
    <div class="sec-head"><h2>우리 강아지 기록과 안전 수칙</h2></div>
    <section class="home-lower">
      <div class="table-panel">
        <div class="panel-head">
          <h2>최근 응급 체크</h2>
          <RouterLink class="panel-more-link" to="/reports">더보기</RouterLink>
        </div>
        <table>
          <thead><tr><th>반려견</th><th>응급도</th><th>증상</th><th>권장 행동</th></tr></thead>
          <tbody>
            <tr v-for="check in recent" :key="check.id">
              <td><div class="pet-cell"><span class="pet-ava">{{ petInitial(check.petName) }}</span>{{ check.petName || "미선택" }}</div></td>
              <td><span class="badge" :class="riskClass(check.riskLevel)">{{ check.riskLevel }}</span></td>
              <td class="sym"><span class="table-line-clamp">{{ check.symptomNote }}</span></td>
              <td class="act"><span class="table-line-clamp">{{ check.recommendedAction }}</span></td>
            </tr>
            <tr v-if="recent.length === 0">
              <td colspan="4">
                <div class="dashboard-empty-cta">
                  <strong>아직 응급 체크 내역이 없습니다.</strong>
                  <p>응급 체크를 한 번 진행하면 반려견, 응급도, 권장 행동이 이 표에 바로 쌓입니다.</p>
                  <RouterLink class="primary-link" to="/emergency">첫 응급 체크 시작하기</RouterLink>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-panel danger-panel">
        <div class="panel-head"><h2>이럴 땐 바로 병원으로</h2></div>
        <ul class="danger-list">
          <li><span class="dl-ic"></span>호흡이 가쁘거나 잇몸·혀가 파랗게 변할 때</li>
          <li><span class="dl-ic"></span>경련하거나 의식이 흐려질 때</li>
          <li><span class="dl-ic"></span>구토·설사가 반복되고 기운이 없을 때</li>
          <li><span class="dl-ic"></span>다량 출혈이나 큰 외상이 있을 때</li>
          <li><span class="dl-ic"></span>초콜릿·포도 등 위험 음식을 삼켰을 때</li>
        </ul>
        <RouterLink class="danger-cta" to="/emergency">지금 응급도 체크하기 →</RouterLink>
      </div>
    </section>
  </main>
</template>
