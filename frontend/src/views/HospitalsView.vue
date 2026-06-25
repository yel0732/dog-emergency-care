<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { hospitalApi } from "../api/resources";
import AppToast from "../components/AppToast.vue";
import CustomSelect from "../components/CustomSelect.vue";
import { formatNotice } from "../utils/messageFormat";

const KAKAO_JS_KEY = import.meta.env.VITE_KAKAO_JS_KEY || "";
const DEFAULT_MAP_CENTER = { lat: 36.5, lng: 127.8 }; // 위치 권한이 없을 때만 쓰는 전국 fallback
const DEFAULT_MAP_LEVEL = 7;
const CURRENT_LOCATION_MAP_LEVEL = 4;
const MAX_LOCATION_ACCURACY_METERS = 50000;
const KOREA_LOCATION_BOUNDS = {
  minLat: 33,
  maxLat: 39,
  minLng: 124,
  maxLng: 132,
};
const REGION_MAP_OVERVIEWS = {
  서울특별시: { lat: 37.535, lng: 127.005, level: 8 },
};

const route = useRoute();
const router = useRouter();
const hospitals = ref([]);
const visibleMapHospitals = ref([]);
const hospitalLoading = ref(false);
const selected = ref(null);
const detailHospital = ref(null);
const detailLoading = ref(false);
const detailError = ref("");
const detailTarget = ref(null);
const toast = reactive({ show: false, type: "info", message: "" });
const mapMessage = ref("");
const mapLoading = ref(false);
const loadError = ref("");
const mapEl = ref(null);
const hospitalListEl = ref(null);
const currentLocation = ref(null);
const regionOptions = reactive({ sidos: [], sigungus: {}, counts: [] });
const filter = reactive({
  keyword: "",
  lat: "",
  lng: "",
  status: "open",
  sido: "",
  sigungu: "",
  phoneOnly: false,
  locatedOnly: false,
});

let kakaoMap = null;
let markers = [];
let currentLocationOverlay = null;
let hospitalInfoOverlay = null;
let toastTimer = null;
let autoLocationRequested = false;

const locatedHospitals = computed(() => hospitals.value.filter(hasCoordinate));
const visibleLocatedCount = computed(() => locatedHospitals.value.length);
const mapPageHospitals = computed(() => visibleMapHospitals.value);
const listedHospitals = computed(() => {
  if (mapPageHospitals.value.length > 0) return prioritizeSelectedHospital(mapPageHospitals.value);
  if (usesDistanceFallbackList()) return prioritizeSelectedHospital(hospitals.value);
  return [];
});
const mapVisibleHospitalCount = computed(() => mapPageHospitals.value.length);
const mapViewportCountText = computed(() => {
  if (hospitalLoading.value) return "불러오는 중";
  if (mapPageHospitals.value.length > 0) return `현재 지도 ${mapPageHospitals.value.length.toLocaleString()} 곳`;
  if (listedHospitals.value.length > 0) return `거리순 ${listedHospitals.value.length.toLocaleString()} 곳`;
  return "현재 지도 0 곳";
});
const mapVisibleEmergency24Count = computed(() =>
  (mapPageHospitals.value.length > 0 ? mapPageHospitals.value : listedHospitals.value).filter(isEmergency24Hospital).length
);
const totalHospitalCount = 5439;
const statusOptions = [
  { value: "open", label: "지금 영업" },
  { value: "normal", label: "일반" },
  { value: "emergency24", label: "응급 24시" },
  { value: "night", label: "야간" },
  { value: "weekend", label: "주말" },
];
const sidoDisplayOrder = [
  "서울특별시",
  "경기도",
  "부산광역시",
  "인천광역시",
  "대구광역시",
  "대전광역시",
  "광주광역시",
  "울산광역시",
  "세종특별자치시",
  "강원특별자치도",
  "충청북도",
  "충청남도",
  "전북특별자치도",
  "전라북도",
  "전라남도",
  "경상북도",
  "경상남도",
  "제주특별자치도",
];
const shortSidoNames = {
  서울특별시: "서울",
  부산광역시: "부산",
  대구광역시: "대구",
  인천광역시: "인천",
  광주광역시: "광주",
  대전광역시: "대전",
  울산광역시: "울산",
  세종특별자치시: "세종",
  강원특별자치도: "강원도",
  전북특별자치도: "전라북도",
  제주특별자치도: "제주도",
};
const fullSidoOptions = computed(() => {
  const merged = new Set([
    ...regionOptions.sidos,
    ...regionOptions.counts.map((region) => region.sido).filter(Boolean),
  ]);
  return [...merged].sort((a, b) => {
    const aIndex = sidoDisplayOrder.indexOf(a);
    const bIndex = sidoDisplayOrder.indexOf(b);
    if (aIndex === -1 && bIndex === -1) return a.localeCompare(b, "ko");
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });
});
const currentSigungus = computed(() => (filter.sido ? regionOptions.sigungus[filter.sido] || [] : []));
const sidoSelectOptions = computed(() => fullSidoOptions.value.map((sido) => ({ value: sido, label: sido })));
const sigunguSelectOptions = computed(() => currentSigungus.value.map((sigungu) => ({ value: sigungu, label: sigungu })));
const regionCards = computed(() => {
  return [...regionOptions.counts].sort((a, b) => {
    const aIndex = sidoDisplayOrder.indexOf(a.sido);
    const bIndex = sidoDisplayOrder.indexOf(b.sido);
    if (aIndex === -1 && bIndex === -1) return a.sido.localeCompare(b.sido, "ko");
    if (aIndex === -1) return 1;
    if (bIndex === -1) return -1;
    return aIndex - bIndex;
  });
});
const isCurrentLocationMode = computed(() => {
  if (!currentLocation.value) return false;
  if (filter.keyword.trim() || filter.sido || filter.sigungu) return false;
  const lat = Number(filter.lat);
  const lng = Number(filter.lng);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return false;
  return Math.abs(lat - currentLocation.value.lat) < 0.0008
    && Math.abs(lng - currentLocation.value.lng) < 0.0008;
});
const hospitalListTitle = computed(() => {
  if (isCurrentLocationMode.value) return "내 주변 동물병원";
  if (filter.sigungu) return `${filter.sigungu} 동물병원`;
  if (filter.sido) return `${sidoLabel(filter.sido)} 동물병원`;
  if (filter.keyword.trim()) return "검색 결과 동물병원";
  return "지도 화면 동물병원";
});

function usesDistanceFallbackList() {
  return isCurrentLocationMode.value || filter.status === "emergency24";
}

function hasCoordinate(hospital) {
  return hospital?.lat != null && hospital?.lng != null;
}

function distanceText(hospital) {
  return hospital.distanceKm != null ? `${Number(hospital.distanceKm).toFixed(2)}km` : "";
}

function verifiedText(value) {
  if (!value) return "검증 이력 없음";
  return String(value).replace("T", " ").slice(0, 16);
}

function sidoLabel(sido) {
  return shortSidoNames[sido] || sido;
}

function showToast(message, type = "info") {
  toast.message = formatNotice(message);
  toast.type = type;
  toast.show = true;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toast.show = false;
  }, 3600);
}

function kakaoRouteLabel(value) {
  return encodeURIComponent(String(value || "동물병원").replace(/[,\r\n]+/g, " ").trim());
}

function directionsUrl(hospital) {
  const query = encodeURIComponent(`${hospital.name} ${hospital.address}`);
  if (hasCoordinate(hospital)) {
    if (currentLocation.value?.lat && currentLocation.value?.lng) {
      return `https://map.kakao.com/link/from/${kakaoRouteLabel("현재 위치")},${currentLocation.value.lat},${currentLocation.value.lng}/to/${kakaoRouteLabel(hospital.name)},${hospital.lat},${hospital.lng}`;
    }
    return `https://map.kakao.com/link/to/${kakaoRouteLabel(hospital.name)},${hospital.lat},${hospital.lng}`;
  }
  return `https://map.kakao.com/link/search/${query}`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function hasSyncedHours(hospital) {
  const openingHours = String(hospital?.openingHours || "").trim();
  return Boolean(
    openingHours
    && hospital.openingHours !== "영업시간 확인 필요"
  );
}

function hasExplicit24hText(hospital) {
  const source = [
    hospital?.name,
    hospital?.hospitalName,
    hospital?.openingHours,
  ].filter(Boolean).join(" ");
  return /(?:24\s*시|24\s*시간|24\s*h|24\/7|open\s*24\s*hours)/i.test(source);
}

function isEmergency24Hospital(hospital) {
  return Boolean(hospital?.is24h || hospital?.emergencyAvailable || hasExplicit24hText(hospital));
}

function normalizedOpeningHours(value) {
  let normalized = String(value || "").trim();
  if (normalized.startsWith('"') && normalized.endsWith('"')) {
    try {
      normalized = JSON.parse(normalized);
    } catch {
      normalized = normalized.replace(/^"+|"+$/g, "");
    }
  }
  return String(normalized || "")
    .trim()
    .replace(/^"+|"+$/g, "")
    .replace(/\\\\r\\\\n|\\\\n|\\\\r/g, "\n")
    .replace(/\\r\\n|\\n|\\r/g, "\n")
    .replace(/\\+r\\+n|\\+n|\\+r/g, "\n")
    .replace(/\s*(?=(?:\uC6D4\uC694\uC77C|\uD654\uC694\uC77C|\uC218\uC694\uC77C|\uBAA9\uC694\uC77C|\uAE08\uC694\uC77C|\uD1A0\uC694\uC77C|\uC77C\uC694\uC77C):)/g, "\n")
    .trim();
}

function openingHoursPreview(hospital, maxLines = 2) {
  if (hasExplicit24hText(hospital)) return ["24시간 운영"];
  if (!hasSyncedHours(hospital)) return ["영업시간 확인 필요"];
  return normalizedOpeningHours(hospital.openingHours)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, maxLines);
}

function openingHoursSummary(hospital) {
  return openingHoursPreview(hospital, 2).join("\n");
}

function openingHourLineClass(line) {
  if (/^\s*\uD1A0\uC694\uC77C:/.test(String(line || ""))) return "week-saturday";
  if (/^\s*\uC77C\uC694\uC77C:/.test(String(line || ""))) return "week-sunday";
  return "";
}

function availabilityBadges(hospital) {
  const explicit24h = hasExplicit24hText(hospital);
  const emergency = isEmergency24Hospital(hospital);
  const night = Boolean(hospital?.nightAvailable || explicit24h);
  const weekend = Boolean(hospital?.holidayAvailable || explicit24h);
  const badges = [];
  if (emergency) badges.push({ label: "응급 24시", kind: "emergency24" });
  if (night) badges.push({ label: "야간", kind: "night" });
  if (weekend) badges.push({ label: "주말", kind: "weekend" });
  if (badges.length === 0) badges.push({ label: "일반", kind: "normal" });
  return badges;
}

function resetHospitalFilters() {
  filter.keyword = "";
  filter.lat = currentLocation.value ? String(currentLocation.value.lat) : "";
  filter.lng = currentLocation.value ? String(currentLocation.value.lng) : "";
  filter.status = "open";
  filter.sido = "";
  filter.sigungu = "";
  filter.phoneOnly = false;
  filter.locatedOnly = false;
  load();
}

function hasExplicitSearchCondition() {
  return Boolean(
    filter.keyword.trim()
    || filter.sido
    || filter.sigungu
    || filter.phoneOnly
    || filter.locatedOnly
    || filter.status !== "open"
  );
}

function searchHospitals() {
  if (!hasExplicitSearchCondition()) {
    showToast("검색어를 입력하거나 지역·영업 조건을 선택해 주세요.", "error");
    return;
  }
  load();
}

function loadKakaoMapScript() {
  if (window.kakao?.maps) return Promise.resolve();
  if (!KAKAO_JS_KEY.trim()) return Promise.reject(new Error("Missing Kakao JavaScript key"));
  const existing = document.querySelector("script[data-kakao-map-sdk]");
  if (existing) {
    if (existing.dataset.kakaoMapSdkStatus === "error" || existing.dataset.kakaoMapSdkStatus === "loaded") {
      existing.remove();
    } else {
      return new Promise((resolve, reject) => {
        existing.addEventListener("load", resolve, { once: true });
        existing.addEventListener("error", reject, { once: true });
      });
    }
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    const retryKey = Date.now();
    script.dataset.kakaoMapSdk = "true";
    script.dataset.kakaoMapSdkStatus = "loading";
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}&autoload=false&retry=${retryKey}`;
    script.onload = () => {
      script.dataset.kakaoMapSdkStatus = "loaded";
      resolve();
    };
    script.onerror = (event) => {
      script.dataset.kakaoMapSdkStatus = "error";
      reject(event);
    };
    document.head.appendChild(script);
  });
}

async function initMap() {
  if (!mapEl.value) return;
  mapLoading.value = true;
  mapMessage.value = "";
  try {
    await loadKakaoMapScript();
    window.kakao.maps.load(() => {
      const initialLat = Number(currentLocation.value?.lat ?? filter.lat);
      const initialLng = Number(currentLocation.value?.lng ?? filter.lng);
      const centerLat = Number.isFinite(initialLat) ? initialLat : DEFAULT_MAP_CENTER.lat;
      const centerLng = Number.isFinite(initialLng) ? initialLng : DEFAULT_MAP_CENTER.lng;
      const center = new window.kakao.maps.LatLng(centerLat, centerLng);

      kakaoMap = new window.kakao.maps.Map(mapEl.value, {
        center,
        level: currentLocation.value ? CURRENT_LOCATION_MAP_LEVEL : DEFAULT_MAP_LEVEL,
      });
      const zoomControl = new window.kakao.maps.ZoomControl();
      kakaoMap.addControl(zoomControl, window.kakao.maps.ControlPosition.RIGHT);
      window.kakao.maps.event.addListener(kakaoMap, "idle", updateVisibleMapHospitals);
      mapMessage.value = "";
      kakaoMap.relayout();
      renderMarkers();
      renderCurrentLocationMarker();
      mapLoading.value = false;
    });
  } catch (error) {
    // 지도 SDK가 실패해도 병원 목록과 Kakao 길찾기 링크는 계속 사용할 수 있게 둔다.
    const isMissingKey = error?.message === "Missing Kakao JavaScript key";
    mapMessage.value = isMissingKey
      ? "카카오 JavaScript 키가 없어 지도만 숨겼습니다. 목록과 길찾기는 사용할 수 있습니다."
      : "카카오 지도 SDK를 불러오지 못했어요. 카카오 개발자 콘솔에서 지도/로컬 서비스 활성화와 localhost 도메인 등록을 확인해 주세요.";
    showToast(
      isMissingKey
        ? "카카오 JavaScript 키가 없어 지도만 숨겼습니다."
        : "카카오 지도 SDK 로딩 실패: 지도/로컬 서비스와 도메인을 확인해 주세요.",
      "error",
    );
    mapLoading.value = false;
  }
}


async function retryMap() {
  mapMessage.value = "";
  if (!kakaoMap) {
    await initMap();
    return;
  }
  renderMarkers();
}

function clearMarkers() {
  markers.forEach((marker) => marker.setMap(null));
  markers = [];
  closeHospitalInfoOverlay();
}

function closeHospitalInfoOverlay() {
  if (hospitalInfoOverlay) {
    hospitalInfoOverlay.setMap(null);
    hospitalInfoOverlay = null;
  }
}

function clearCurrentLocationMarker() {
  if (currentLocationOverlay) {
    currentLocationOverlay.setMap(null);
    currentLocationOverlay = null;
  }
}

function renderCurrentLocationMarker() {
  if (!kakaoMap || !window.kakao?.maps) return;
  clearCurrentLocationMarker();
  if (!isCurrentLocationMode.value) return;

  const position = new window.kakao.maps.LatLng(currentLocation.value.lat, currentLocation.value.lng);
  currentLocationOverlay = new window.kakao.maps.CustomOverlay({
    position,
    xAnchor: 0.5,
    yAnchor: 0.5,
    zIndex: 20,
    content: `
      <div class="my-location-marker" title="내 위치">
        <span class="my-location-pulse"></span>
        <span class="my-location-pin" aria-hidden="true"></span>
        <strong>내 위치</strong>
      </div>
    `,
  });
  currentLocationOverlay.setMap(kakaoMap);
}

function showEmptyMapGuide() {
  if (filter.keyword.trim()) {
    mapMessage.value = "현재 위치를 확인하지 못했습니다.\n브라우저 위치 권한을 허용하거나 지역을 선택해 주세요.";
    return;
  }

  if (filter.sido || filter.sigungu || filter.status !== "open" || filter.phoneOnly || filter.locatedOnly) {
    mapMessage.value = "선택한 조건에 맞는 병원이 없습니다.\n조건을 줄이거나 다른 지역을 선택해 주세요.";
    return;
  }

  mapMessage.value = "지도에 표시할 병원이 없습니다.\n현위치 새로고침 또는 지역 선택을 이용해 주세요.";
}

function createHospitalMarker(hospital, bounds) {
  const position = new window.kakao.maps.LatLng(Number(hospital.lat), Number(hospital.lng));
  const marker = new window.kakao.maps.Marker({
    map: kakaoMap,
    position,
    title: hospital.name,
  });

  window.kakao.maps.event.addListener(marker, "click", () => {
    focusHospital(hospital, true);
  });

  markers.push(marker);
  bounds.extend(position);
}

function fitHospitalMapBounds(located, bounds) {
  if (!filter.sido && !filter.sigungu && !filter.keyword.trim()) {
    const lat = Number(filter.lat);
    const lng = Number(filter.lng);
    const center = Number.isFinite(lat) && Number.isFinite(lng)
      ? { lat, lng }
      : DEFAULT_MAP_CENTER;
    kakaoMap.setCenter(new window.kakao.maps.LatLng(center.lat, center.lng));
    kakaoMap.setLevel(currentLocation.value ? CURRENT_LOCATION_MAP_LEVEL : DEFAULT_MAP_LEVEL);
    closeHospitalInfoOverlay();
    return currentLocation.value ? "current" : "default";
  }
  const regionOverview = REGION_MAP_OVERVIEWS[filter.sido];
  if (regionOverview && !filter.sigungu && !filter.keyword.trim()) {
    kakaoMap.setCenter(new window.kakao.maps.LatLng(regionOverview.lat, regionOverview.lng));
    kakaoMap.setLevel(regionOverview.level);
    closeHospitalInfoOverlay();
    return "region";
  }
  if (located.length === 1) {
    kakaoMap.setCenter(new window.kakao.maps.LatLng(Number(located[0].lat), Number(located[0].lng)));
    kakaoMap.setLevel(CURRENT_LOCATION_MAP_LEVEL);
    return "focused";
  }
  if (selected.value && hasCoordinate(selected.value)) {
    kakaoMap.setCenter(new window.kakao.maps.LatLng(Number(selected.value.lat), Number(selected.value.lng)));
    kakaoMap.setLevel(CURRENT_LOCATION_MAP_LEVEL);
    return "focused";
  }
  kakaoMap.setBounds(bounds);
  return "bounds";
}

function focusMapForEmptyResults() {
  if (!kakaoMap || !window.kakao?.maps) return;

  const lat = Number(filter.lat);
  const lng = Number(filter.lng);
  if (currentLocation.value && Number.isFinite(lat) && Number.isFinite(lng)) {
    kakaoMap.setCenter(new window.kakao.maps.LatLng(lat, lng));
    kakaoMap.setLevel(CURRENT_LOCATION_MAP_LEVEL);
    renderCurrentLocationMarker();
    return;
  }

  const regionOverview = REGION_MAP_OVERVIEWS[filter.sido];
  if (regionOverview) {
    kakaoMap.setCenter(new window.kakao.maps.LatLng(regionOverview.lat, regionOverview.lng));
    kakaoMap.setLevel(regionOverview.level);
    return;
  }

  kakaoMap.setCenter(new window.kakao.maps.LatLng(DEFAULT_MAP_CENTER.lat, DEFAULT_MAP_CENTER.lng));
  kakaoMap.setLevel(DEFAULT_MAP_LEVEL);
}

function hospitalPosition(hospital) {
  return new window.kakao.maps.LatLng(Number(hospital.lat), Number(hospital.lng));
}

function prioritizeSelectedHospital(list) {
  if (!selected.value?.id) return list;
  const selectedIndex = list.findIndex((hospital) => hospital.id === selected.value.id);
  if (selectedIndex <= 0) return list;
  const copy = [...list];
  const [picked] = copy.splice(selectedIndex, 1);
  copy.unshift(picked);
  return copy;
}

function updateVisibleMapHospitals() {
  if (!kakaoMap || !window.kakao?.maps) {
    visibleMapHospitals.value = locatedHospitals.value;
    return;
  }
  const bounds = kakaoMap.getBounds();
  visibleMapHospitals.value = locatedHospitals.value.filter((hospital) =>
    bounds.contain(hospitalPosition(hospital))
  );
}

function focusSelectedHospitalOnMap() {
  if (selected.value && hasCoordinate(selected.value)) {
    focusHospital(selected.value, false);
  }
}

function renderMarkers() {
  if (!kakaoMap || !window.kakao?.maps) return;
  clearMarkers();
  kakaoMap.relayout();
  renderCurrentLocationMarker();

  const located = locatedHospitals.value;
  if (located.length === 0) {
    visibleMapHospitals.value = [];
    focusMapForEmptyResults();
    showEmptyMapGuide();
    return;
  }

  mapMessage.value = "";
  const bounds = new window.kakao.maps.LatLngBounds();
  located.forEach((hospital) => createHospitalMarker(hospital, bounds));
  const mapFitMode = fitHospitalMapBounds(located, bounds);
  if (mapFitMode !== "region") focusSelectedHospitalOnMap();
  window.setTimeout(updateVisibleMapHospitals, 120);
}

function openInfoWindow(hospital, marker) {
  if (!kakaoMap || !marker) return;
  closeHospitalInfoOverlay();
  const badges = availabilityBadges(hospital)
    .map((badge) => `<span class="info-badge ${badge.kind}">${escapeHtml(badge.label)}</span>`)
    .join("");
  const hours = openingHoursPreview(hospital, 7)
    .map((line) => `<span class="info-hour ${openingHourLineClass(line)}">${escapeHtml(line)}</span>`)
    .join("");
  const content = `
    <div class="kakao-info-window">
      <strong class="info-title">${escapeHtml(hospital.name)}</strong>
      <div class="info-badges">${badges}</div>
      <span class="info-address">${escapeHtml(hospital.address)}</span>
      <div class="info-hours">${hours}</div>
      <a class="info-route-link" href="${directionsUrl(hospital)}" target="_blank" rel="noopener noreferrer">Kakao 길찾기</a>
    </div>
  `;
  markers.forEach((item) => item.setZIndex?.(1));
  marker.setZIndex?.(20);
  hospitalInfoOverlay = new window.kakao.maps.CustomOverlay({
    position: marker.getPosition(),
    content,
    xAnchor: 0.5,
    yAnchor: 1.15,
    zIndex: 60,
  });
  hospitalInfoOverlay.setMap(kakaoMap);
}

function focusHospital(hospital, pan = true) {
  selected.value = hospital;
  scrollSelectedHospitalIntoList();
  if (!kakaoMap || !hasCoordinate(hospital)) return;

  const position = hospitalPosition(hospital);
  if (pan) {
    kakaoMap.panTo(position);
    window.setTimeout(() => {
      const projection = kakaoMap?.getProjection?.();
      if (!projection) return;
      const point = projection.containerPointFromCoords(position);
      point.y -= 90;
      kakaoMap.panTo(projection.coordsFromContainerPoint(point));
    }, 180);
  }

  const marker = markers.find((item) => item.getTitle() === hospital.name);
  if (marker) openInfoWindow(hospital, marker);
}

async function scrollSelectedHospitalIntoList() {
  await nextTick();
  if (!hospitalListEl.value || !selected.value) return;
  const card = hospitalListEl.value.querySelector(`[data-hospital-id="${selected.value.id}"]`);
  if (!card) return;
  hospitalListEl.value.scrollTo({
    top: 0,
    behavior: "smooth",
  });
}

function centerCurrentLocation() {
  if (!currentLocation.value || !kakaoMap || !window.kakao?.maps) {
    showToast("현재 위치를 아직 확인하지 못했어요.\n브라우저 위치 권한을 확인해 주세요.", "error");
    return;
  }
  const position = new window.kakao.maps.LatLng(currentLocation.value.lat, currentLocation.value.lng);
  kakaoMap.setCenter(position);
  kakaoMap.setLevel(CURRENT_LOCATION_MAP_LEVEL);
  renderCurrentLocationMarker();
}

function resetCurrentLocationFilters() {
  filter.keyword = "";
  filter.sido = "";
  filter.sigungu = "";
  filter.phoneOnly = false;
  filter.locatedOnly = false;
}

async function loadAroundCurrentLocation({ notify = false } = {}) {
  if (currentLocation.value) {
    resetCurrentLocationFilters();
    filter.lat = String(currentLocation.value.lat);
    filter.lng = String(currentLocation.value.lng);
    await load();
    centerCurrentLocation();
    if (notify) showToast("현위치 주변 동물병원을 표시했습니다.", "success");
    return;
  }
  const located = await requestCurrentLocation({ syncFilter: false, reload: false, pan: false, notify });
  if (located && currentLocation.value) {
    resetCurrentLocationFilters();
    filter.lat = String(currentLocation.value.lat);
    filter.lng = String(currentLocation.value.lng);
    await load();
    centerCurrentLocation();
  }
}

async function refreshCurrentLocationHospitals({ notify = true } = {}) {
  await locate();
}

function requestCurrentLocation({ syncFilter = false, reload = false, pan = false, notify = false } = {}) {
  const geolocation = window.navigator?.geolocation;
  if (!geolocation) {
    if (notify) showToast("이 브라우저에서는 현재 위치를 사용할 수 없습니다.", "error");
    return Promise.resolve(false);
  }

  const getPosition = (options) => new Promise((resolve, reject) => {
    geolocation.getCurrentPosition(resolve, reject, options);
  });
  const positionOptions = [
    { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 },
    { enableHighAccuracy: false, timeout: 12000, maximumAge: 30000 },
    { enableHighAccuracy: false, timeout: 16000, maximumAge: 300000 },
  ];

  return (async () => {
    for (const options of positionOptions) {
      try {
        const pos = await getPosition(options);
        const lat = Number(pos.coords.latitude.toFixed(6));
        const lng = Number(pos.coords.longitude.toFixed(6));
        const accuracy = Number(pos.coords.accuracy);
        const isInKorea = lat >= KOREA_LOCATION_BOUNDS.minLat
          && lat <= KOREA_LOCATION_BOUNDS.maxLat
          && lng >= KOREA_LOCATION_BOUNDS.minLng
          && lng <= KOREA_LOCATION_BOUNDS.maxLng;
        const hasUsableAccuracy = !Number.isFinite(accuracy) || accuracy <= MAX_LOCATION_ACCURACY_METERS;
        if (!isInKorea || !hasUsableAccuracy) {
          throw new Error("Unreliable browser location");
        }
        currentLocation.value = { lat, lng, accuracy };
        if (syncFilter) {
          filter.lat = String(lat);
          filter.lng = String(lng);
        }
        if (kakaoMap && window.kakao?.maps) {
          if (pan) centerCurrentLocation();
          else renderCurrentLocationMarker();
        }
        if (reload) {
          await load();
          if (notify) showToast("현위치 기준으로 병원 목록을 갱신했습니다.", "success");
          return true;
        }
        if (notify) showToast("지도에 내 위치를 표시했습니다.", "success");
        return true;
      } catch {
        // Try a less strict browser geolocation strategy.
      }
    }
    currentLocation.value = null;
    clearCurrentLocationMarker();
    if (syncFilter) {
      filter.lat = "";
      filter.lng = "";
    }
    if (notify) {
      showToast("현재 위치를 가져오지 못했어요.\n브라우저 위치 권한을 허용한 뒤 다시 시도해 주세요.", "error");
    }
    return false;
  })();
}

async function locate() {
  currentLocation.value = null;
  clearCurrentLocationMarker();
  const located = await requestCurrentLocation({ syncFilter: false, reload: false, pan: false, notify: true });
  if (!located || !currentLocation.value) return;
  resetCurrentLocationFilters();
  filter.lat = String(currentLocation.value.lat);
  filter.lng = String(currentLocation.value.lng);
  await load();
  centerCurrentLocation();
}
function syncQueryToUrl() {
  const query = {};
  if (filter.keyword) query.keyword = filter.keyword;
  if (filter.status && filter.status !== "open") query.status = filter.status;
  if (filter.sido) query.sido = filter.sido;
  if (filter.sigungu) query.sigungu = filter.sigungu;
  if (filter.phoneOnly) query.phone = "1";
  if (filter.locatedOnly) query.located = "1";
  router.replace({ query }).catch(() => {});
}

async function load(options = {}) {
  try {
    hospitalLoading.value = true;
    loadError.value = "";
    hospitals.value = await hospitalApi.list({
      keyword: filter.keyword || undefined,
      lat: filter.lat || undefined,
      lng: filter.lng || undefined,
      status: filter.status || undefined,
      sido: filter.sido || undefined,
      sigungu: filter.sigungu || undefined,
      phoneOnly: filter.phoneOnly || undefined,
      locatedOnly: filter.locatedOnly || undefined,
    });
    visibleMapHospitals.value = locatedHospitals.value;
    selected.value = options.keepSelection ? selected.value : null;
    syncQueryToUrl();
    await nextTick();
    renderMarkers();
  } catch (e) {
    hospitals.value = [];
    visibleMapHospitals.value = [];
    selected.value = null;
    loadError.value = "병원 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.";
    showToast(loadError.value, "error");
  } finally {
    hospitalLoading.value = false;
  }
}

async function loadRegions() {
  try {
    const response = await hospitalApi.regions();
    regionOptions.sidos = response.sidos || [];
    regionOptions.sigungus = response.sigungus || {};
    regionOptions.counts = response.counts || [];
  } catch {
    regionOptions.sidos = [];
    regionOptions.sigungus = {};
    regionOptions.counts = [];
  }
}

function selectStatus(status) {
  filter.status = status;
  load();
}

function clearSigunguAndLoad() {
  filter.sigungu = "";
  filter.lat = "";
  filter.lng = "";
  clearCurrentLocationMarker();
  load();
}

function moveToSido(sido) {
  if (filter.sido === sido) {
    loadAroundCurrentLocation({ notify: false });
    return;
  }
  filter.sido = sido;
  filter.sigungu = "";
  filter.lat = "";
  filter.lng = "";
  filter.status = "";
  clearCurrentLocationMarker();
  load();
}

async function openHospitalDetail(hospital) {
  detailTarget.value = hospital;
  detailError.value = "";
  detailHospital.value = null;
  detailLoading.value = true;
  try {
    detailHospital.value = await hospitalApi.find(hospital.id);
    focusHospital(detailHospital.value);
  } catch (e) {
    detailHospital.value = null;
    detailError.value = e.status === 404
      ? "병원 정보를 찾을 수 없습니다. 목록을 다시 불러온 뒤 다른 병원을 선택해 주세요."
      : "병원 상세 정보를 불러오지 못했어요. 목록에서 다시 선택해 주세요.";
    showToast(detailError.value, "error");
    if (e.status === 404) await load();
  } finally {
    detailLoading.value = false;
  }
}

function closeHospitalDetail() {
  detailHospital.value = null;
  detailError.value = "";
  detailTarget.value = null;
}

function retryHospitalDetail() {
  if (detailTarget.value) openHospitalDetail(detailTarget.value);
}

watch(selected, (hospital) => {
  if (hospital) focusHospital(hospital, false);
});

onMounted(async () => {
  const q = route.query;
  if (q.keyword) filter.keyword = String(q.keyword);
  if (q.status) filter.status = String(q.status);
  if (q.sido) filter.sido = String(q.sido);
  if (q.sigungu) filter.sigungu = String(q.sigungu);
  filter.phoneOnly = String(q.phone || "") === "1";
  filter.locatedOnly = String(q.located || "") === "1";
  const hasInitialQuery = Boolean(
    q.keyword
    || q.status
    || q.sido
    || q.sigungu
    || q.phone
    || q.located
  );
  await loadRegions();

  let shouldLoadHospitals = true;
  if (!autoLocationRequested && !hasInitialQuery) {
    autoLocationRequested = true;
    const located = await requestCurrentLocation({ syncFilter: true, reload: false, pan: false, notify: false });
    if (!located) {
      filter.lat = String(DEFAULT_MAP_CENTER.lat);
      filter.lng = String(DEFAULT_MAP_CENTER.lng);
      selected.value = null;
      mapMessage.value = "";
    }
  }

  await nextTick();
  await initMap();

  if (shouldLoadHospitals) {
    await load({ resetMapPage: false, keepSelection: false });
  } else {
    renderMarkers();
  }
  if (!autoLocationRequested) {
    autoLocationRequested = true;
    requestCurrentLocation({ syncFilter: false, reload: false, pan: false, notify: false });
  }
});
</script>

<template>
  <main class="workspace directory-layout hospital-ping-layout">
    <section class="hospital-search-hero">
      <div class="hospital-hero-copy">
        <span class="eyebrow emergency">Emergency hospital map</span>
        <h1>지금 갈 수 있는<br />동물병원 찾기</h1>
        <p>응급, 야간, 주말 진료 가능 여부를 확인하고 현재 위치 주변 병원을 지도에서 바로 비교할 수 있습니다.</p>
      </div>
    </section>

    <section class="hospital-control-panel">
      <form class="hospital-filter-board" novalidate @submit.prevent="searchHospitals">
        <div class="filter-row">
          <span class="filter-label">영업</span>
          <div class="hospital-status-tabs" role="tablist" aria-label="영업 필터">
            <button
              v-for="option in statusOptions"
              :key="option.value"
              type="button"
              :class="[`status-${option.value}`, { active: filter.status === option.value, urgent: option.value === 'emergency24' }]"
              @click="selectStatus(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <div class="filter-row">
          <span class="filter-label">지역</span>
          <CustomSelect
            v-model="filter.sido"
            class="hospital-region-select"
            :options="sidoSelectOptions"
            placeholder="시·도 선택"
            aria-label="시·도 선택"
            @change="clearSigunguAndLoad"
          />
          <CustomSelect
            v-model="filter.sigungu"
            class="hospital-region-select"
            :class="{ disabled: !filter.sido }"
            :options="sigunguSelectOptions"
            placeholder="시·군·구 선택"
            aria-label="시·군·구 선택"
            @change="load"
          />
        </div>

        <div class="filter-row search-row">
          <span class="filter-label">검색</span>
          <div class="hospital-search-input">
            <span class="search-magnifier-wrap" aria-hidden="true">
              <svg class="search-magnifier-icon" viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="7" />
                <path d="m21 21-4.3-4.3" />
              </svg>
            </span>
            <input v-model="filter.keyword" placeholder="병원 이름으로 검색" />
          </div>
          <button class="primary">검색</button>
          <button type="button" class="reset-filter-button" @click="resetHospitalFilters">초기화</button>
        </div>

        <div class="filter-row option-row">
          <span class="filter-label">옵션</span>
          <label class="hospital-check">
            <input v-model="filter.phoneOnly" type="checkbox" @change="load" />
            전화 가능
          </label>
          <label class="hospital-check">
            <input v-model="filter.locatedOnly" type="checkbox" @change="load" />
            지도 표시
          </label>
        </div>
      </form>
    </section>

    <AppToast
      :show="toast.show"
      :type="toast.type"
      :message="toast.message"
      @close="toast.show = false"
    />

    <section class="hospital-region-section" aria-labelledby="hospital-region-title">
      <div class="hospital-region-head">
        <div>
          <span class="eyebrow">Regional directory</span>
          <h2 id="hospital-region-title">시·도별 동물병원</h2>
        </div>
        <span>전국 {{ regionCards.length }}개 시·도</span>
      </div>
      <div class="hospital-region-grid">
        <button
          v-for="region in regionCards"
          :key="region.sido"
          type="button"
          class="hospital-region-card"
          :class="{ active: filter.sido === region.sido }"
          @click="moveToSido(region.sido)"
        >
          <strong>{{ sidoLabel(region.sido) }}</strong>
          <span>{{ Number(region.count).toLocaleString() }}곳</span>
        </button>
      </div>
    </section>

    <section class="hospital-panel" :class="{ 'is-short-list': !hospitalLoading && listedHospitals.length <= 1 }">
      <div class="map-panel" :class="{ 'is-busy': mapLoading || hospitalLoading, 'has-map-message': Boolean(mapMessage) && !mapLoading && !hospitalLoading }">
        <div ref="mapEl" class="kakao-map"></div>
        <div v-if="mapLoading || hospitalLoading" class="map-loading-state" role="status" aria-live="polite">
          <span class="map-loading-spinner" aria-hidden="true"></span>
          <strong>지도를 불러오는 중입니다.</strong>
          <p>현재 위치와 주변 병원 정보를 맞춰 표시하고 있습니다.</p>
        </div>
        <div v-else-if="mapMessage" class="map-message">
          <p>{{ mapMessage }}</p>
          <button type="button" @click="retryMap">지도 다시 시도</button>
        </div>
        <div class="map-status-chips" aria-label="지도 빠른 정보">
          <button
            type="button"
            class="map-status-chip location"
            :class="{ pending: !currentLocation }"
            :title="currentLocation ? '현위치 기준으로 병원 다시 보기' : '현재 위치 표시'"
            @click="refreshCurrentLocationHospitals({ notify: true })"
          >
            <span aria-hidden="true">⌖</span>현위치
          </button>
          <span class="map-status-chip open">
            <span aria-hidden="true"></span>영업 중 {{ mapVisibleHospitalCount.toLocaleString() }}
          </span>
          <span class="map-status-chip urgent">
            <span aria-hidden="true"></span>응급 24시 {{ mapVisibleEmergency24Count.toLocaleString() }}
          </span>
        </div>
      </div>

      <aside ref="hospitalListEl" class="hospital-list">
        <div class="hospital-list-head">
          <div>
            <span class="eyebrow">Map results</span>
            <h2>{{ hospitalListTitle }}</h2>
          </div>
          <div class="hospital-list-tools">
            <strong>{{ mapViewportCountText }}</strong>
          </div>
        </div>
        <div v-if="hospitalLoading" class="hospital-empty-recovery loading">
          <strong>병원 목록 로딩 중입니다.</strong>
          <p>지역과 지도 정보를 맞춰 표시하고 있어요.</p>
        </div>
        <template v-else>
          <article
            v-for="h in listedHospitals"
            :key="h.id"
            class="hospital-card"
            :class="{ selected: selected?.id === h.id }"
            :data-hospital-id="h.id"
            @click="focusHospital(h)"
          >
            <div class="hospital-card-head">
              <strong>{{ h.name }}</strong>
              <span v-if="distanceText(h)" class="coord-badge ready">{{ distanceText(h) }}</span>
            </div>
            <div class="hospital-badge-row">
              <span
                v-for="badge in availabilityBadges(h)"
                :key="badge.label"
                class="hospital-status-badge"
                :class="badge.kind"
              >
                {{ badge.label }}
              </span>
            </div>
            <p>{{ h.address }}</p>
            <ul class="hospital-info-list" aria-label="병원 기본 정보">
              <li class="hospital-info-item">
                <span class="info-label">영업시간</span>
                <span class="info-value hospital-hours-preview">{{ openingHoursSummary(h) }}</span>
              </li>
              <li class="hospital-info-item">
                <span class="info-label">전화</span>
                <span class="info-value phone-line" :class="{ muted: !h.phone }">{{ h.phone || "전화번호 확인 필요" }}</span>
              </li>
            </ul>
            <small v-if="!hasCoordinate(h)" class="hospital-note-line">지도 표시 정보를 준비 중입니다.</small>
            <div class="card-actions hospital-card-actions">
              <button type="button" class="hospital-action" @click.stop="openHospitalDetail(h)">상세</button>
              <a class="hospital-action route" :href="directionsUrl(h)" target="_blank" rel="noreferrer">길찾기</a>
              <a v-if="h.phone" class="hospital-action call icon-only" :href="`tel:${h.phone}`" :aria-label="`${h.name} 전화 걸기`" title="전화 걸기"></a>
            </div>
          </article>
        </template>
        <div v-if="!hospitalLoading && hospitals.length === 0" class="hospital-empty-recovery">
          <strong>항목이 없어요.</strong>
          <p>지역이나 영업 조건을 줄이거나, 기본 위치 기준으로 다시 조회해 보세요.</p>
          <div>
            <button type="button" @click="resetHospitalFilters">조건 초기화</button>
            <button type="button" class="primary" @click="load">다시 검색</button>
          </div>
        </div>
        <div v-else-if="!hospitalLoading && listedHospitals.length === 0" class="hospital-empty-recovery">
          <strong>현재 지도 화면에 표시할 병원이 없어요.</strong>
          <p>지도를 조금 이동하거나 축소해서 다시 확인해 주세요.</p>
          <div>
            <button type="button" @click="resetHospitalFilters">조건 초기화</button>
            <button type="button" class="primary" @click="load">다시 검색</button>
          </div>
        </div>
      </aside>
    </section>

    <div v-if="detailHospital || detailLoading || detailError" class="hospital-detail-backdrop" @click.self="closeHospitalDetail">
      <section class="hospital-detail-modal" role="dialog" aria-modal="true">
        <header>
          <div>
            <span class="eyebrow">Hospital detail</span>
            <h2>{{ detailLoading ? "병원 정보를 불러오는 중" : (detailHospital?.name || "병원 상세 확인") }}</h2>
          </div>
          <button type="button" @click="closeHospitalDetail">닫기</button>
        </header>

        <div v-if="detailLoading" class="hospital-empty-recovery loading">
          <strong>병원 상세 정보를 불러오는 중입니다.</strong>
          <p>목록에서 선택한 병원의 전화번호, 운영 상태, 길찾기 정보를 확인하고 있어요.</p>
        </div>

        <div v-else-if="detailError" class="recovery-panel error hospital-detail-recovery">
          <strong>병원 상세 정보를 불러오지 못했어요.</strong>
          <p>{{ detailError }}</p>
          <div class="recovery-actions">
            <button class="primary" type="button" @click="retryHospitalDetail">다시 시도</button>
            <button type="button" @click="closeHospitalDetail">목록으로 돌아가기</button>
          </div>
        </div>

        <div v-else-if="detailHospital" class="hospital-detail-body">
          <div class="hospital-detail-status-row">
            <span
              v-for="badge in availabilityBadges(detailHospital)"
              :key="badge.label"
              class="hospital-status-badge"
              :class="badge.kind"
            >
              {{ badge.label }}
            </span>
          </div>
          <dl class="hospital-detail-grid">
            <div class="detail-item detail-address">
              <dt>주소</dt>
              <dd>{{ detailHospital.address }}</dd>
            </div>
            <div class="detail-item detail-phone">
              <dt>전화번호</dt>
              <dd>{{ detailHospital.phone || "전화번호 확인 필요" }}</dd>
            </div>
            <div class="detail-item detail-hours">
              <dt>영업 정보</dt>
              <dd class="hospital-hours-full">
                <span
                  v-for="line in openingHoursPreview(detailHospital, 7)"
                  :key="line"
                  class="hospital-hour-line"
                  :class="openingHourLineClass(line)"
                >
                  {{ line }}
                </span>
                <span class="hospital-hour-status">· {{ detailHospital.operatingStatus }}</span>
              </dd>
            </div>
            <div class="detail-item detail-status">
              <dt>응급/야간/주말</dt>
              <dd>
                <span
                  v-for="badge in availabilityBadges(detailHospital)"
                  :key="badge.label"
                  class="hospital-status-badge"
                  :class="badge.kind"
                >
                  {{ badge.label }}
                </span>
              </dd>
            </div>
          </dl>
          <p class="hospital-detail-notice">
            공공데이터 기반 정보입니다. 방문 전 전화로 진료 가능 여부와 운영 시간을 다시 확인해 주세요.
          </p>
          <div class="hospital-detail-actions">
            <a class="hospital-action route" :href="directionsUrl(detailHospital)" target="_blank" rel="noreferrer">Kakao 길찾기</a>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>
