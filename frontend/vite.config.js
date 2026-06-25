import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, "..", "");

  return {
    envDir: "..",
    plugins: [vue()],
    build: {
      outDir: "../src/main/resources/static",
      emptyOutDir: true,
    },
    server: {
      proxy: {
        "/api": {
          target: env.VITE_API_PROXY_TARGET || "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  };
});
