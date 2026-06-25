import { defineStore } from "pinia";
import { setAccessToken } from "../api/client";
import { authApi } from "../api/resources";

export const useAuthStore = defineStore("auth", {
  state: () => ({ me: null, loading: false, error: "" }),
  getters: {
    isLoggedIn: (state) => Boolean(state.me),
  },
  actions: {
    async loadMe() {
      this.loading = true;
      this.error = "";
      try {
        this.me = await authApi.me();
      } catch {
        this.me = null;
        setAccessToken("");
      } finally {
        this.loading = false;
      }
    },
    async login(credentials) {
      this.error = "";
      const response = await authApi.login(credentials);
      setAccessToken(response.accessToken);
      this.me = response.user;
    },
    async logout() {
      try {
        await authApi.logout();
      } catch {
        // 서버 블랙리스트 등록을 먼저 시도하고, 실패해도 로컬 토큰은 반드시 삭제해 재사용을 막는다.
      } finally {
        setAccessToken("");
      }
      this.me = null;
    },
  },
});
