import { createRouter, createWebHistory } from "vue-router";
import { getAccessToken } from "../api/client";
import { useAuthStore } from "../stores/auth";
import DashboardView from "../views/DashboardView.vue";
import EmergencyView from "../views/EmergencyView.vue";
import FoodSafetyView from "../views/FoodSafetyView.vue";
import HospitalsView from "../views/HospitalsView.vue";
import CaseBoardView from "../views/CaseBoardView.vue";
import LoginView from "../views/LoginView.vue";
import PetsView from "../views/PetsView.vue";
import ReportsView from "../views/ReportsView.vue";
import UsersView from "../views/UsersView.vue";
import VideosView from "../views/VideosView.vue";
import NotFoundView from "../views/NotFoundView.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: DashboardView },
    { path: "/login", component: LoginView },
    { path: "/users", component: UsersView, meta: { requiresAuth: true } },
    { path: "/favorites", component: UsersView, meta: { requiresAuth: true, tab: "favorites" } },
    { path: "/follow", component: UsersView, meta: { requiresAuth: true, tab: "follow" } },
    { path: "/followers", component: UsersView, meta: { requiresAuth: true, tab: "follow" } },
    { path: "/following", component: UsersView, meta: { requiresAuth: true, tab: "follow" } },
    { path: "/pets", redirect: { path: "/users", query: { tab: "pets" } }, meta: { requiresAuth: true } },
    { path: "/records", component: PetsView, meta: { requiresAuth: true, tab: "plans" } },
    { path: "/videos", component: VideosView },
    { path: "/emergency", component: EmergencyView, meta: { requiresAuth: true } },
    { path: "/cases", component: CaseBoardView, meta: { requiresAuth: true } },
    { path: "/cases/:id", component: CaseBoardView, meta: { requiresAuth: true } },
    { path: "/reports", component: ReportsView, meta: { requiresAuth: true } },
    { path: "/hospitals", component: HospitalsView, meta: { requiresAuth: true } },
    { path: "/food-safety", component: FoodSafetyView },
    { path: "/:pathMatch(.*)*", component: NotFoundView },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuthStore();

  if (getAccessToken() && !auth.me && !auth.loading) {
    try {
      await auth.loadMe();
    } catch (err) {
      console.error("인증 정보를 불러오는데 실패했습니다.");
    }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  if (to.path === "/login" && auth.isLoggedIn) return "/";
  return true;
});
