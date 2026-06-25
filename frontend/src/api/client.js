import axios from "axios";

const TOKEN_STORAGE_KEY = "mungz_access_token";
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

export function getAccessToken() {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function setAccessToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_STORAGE_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  }
}

function filenameFromContentDisposition(value) {
  const header = String(value || "");
  const encoded = header.match(/filename\*=UTF-8''([^;]+)/i)?.[1];
  if (encoded) {
    try {
      return decodeURIComponent(encoded);
    } catch {
      return encoded;
    }
  }
  return header.match(/filename="?([^"]+)"?/i)?.[1] || "";
}

export async function apiDownload(url, fallbackFilename) {
  const response = await apiClient.get(url, { responseType: "blob" });
  const filename = filenameFromContentDisposition(response.headers["content-disposition"]) || fallbackFilename || "download";
  const blobUrl = URL.createObjectURL(response.data);
  const link = document.createElement("a");
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(blobUrl);
}

function responseMessage(error) {
  const body = error.response?.data;
  if (Array.isArray(body?.errors) && body.errors.length > 0) {
    return body.errors.map((item) => `${item.field}: ${item.message}`).join("\n");
  }
  if (typeof body === "string" && body.trim()) {
    return body.trim();
  }
  return body?.message || body?.error || "";
}

function normalizeUserMessage(message, status) {
  const clean = String(message || "").trim();
  if (!clean) return "";
  if (/internal server error/i.test(clean)) {
    return "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }
  if (/network error|request failed/i.test(clean)) {
    return "서버에 연결하지 못했습니다. 서버 실행 여부와 네트워크 상태를 확인해 주세요.";
  }
  if (status === 500 && clean.length < 4) {
    return "서버에서 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.";
  }
  return clean;
}

function normalizeApiError(error) {
  const status = error.response?.status;
  const serverMessage = normalizeUserMessage(responseMessage(error), status);
  const message = error.response
    ? serverMessage || `요청 처리에 실패했습니다. (${status})`
    : "서버에 연결하지 못했습니다. 서버 실행 여부와 네트워크 상태를 확인해 주세요.";
  const normalized = new Error(message, { cause: error });
  normalized.status = status;
  normalized.response = error.response;
  normalized.code = error.code;
  normalized.originalMessage = error.message;
  return normalized;
}

function notifyAuthExpired(error) {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent("auth:expired", {
    detail: {
      status: error.status,
      code: error.code,
      message: error.message,
    },
  }));
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const requestUrl = error.config?.url || "";
    if (status === 401 && !requestUrl.includes("/auth/login") && !requestUrl.includes("/auth/me")) {
      const normalized = new Error("로그인이 만료되었습니다. 다시 로그인해 주세요.", { cause: error });
      normalized.status = status;
      normalized.response = error.response;
      normalized.code = error.code;
      normalized.originalMessage = error.message;
      setAccessToken("");
      notifyAuthExpired(normalized);
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        const redirect = `${window.location.pathname}${window.location.search}`;
        window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`);
      }
      return Promise.reject(normalized);
    }
    return Promise.reject(normalizeApiError(error));
  }
);

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function apiGet(url, config) {
  const { data } = await apiClient.get(url, config);
  return data;
}

export async function apiPost(url, body) {
  const { data } = await apiClient.post(url, body);
  return data;
}

export async function apiPut(url, body) {
  const { data } = await apiClient.put(url, body);
  return data;
}

export async function apiPatch(url, body) {
  const { data } = await apiClient.patch(url, body);
  return data;
}

export async function apiDelete(url) {
  const { data } = await apiClient.delete(url);
  return data;
}
