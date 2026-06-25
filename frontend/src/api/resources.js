import { apiDelete, apiDownload, apiGet, apiPatch, apiPost, apiPut } from "./client";

export const authApi = {
  login: (body) => apiPost("/auth/login", body),
  logout: () => apiPost("/auth/logout", {}),
  me: () => apiGet("/auth/me"),
};

export const userApi = {
  create: (body) => apiPost("/users", body),
  availability: (params = {}) => apiGet("/users/availability", { params }),
  updateMe: (body) => apiPut("/users/me", body),
  removeMe: () => apiDelete("/users/me"),
  follow: (targetId) => apiPost(`/users/${targetId}/follow`, {}),
  unfollow: (targetId) => apiDelete(`/users/${targetId}/follow`),
  followers: () => apiGet("/users/me/followers"),
  following: () => apiGet("/users/me/following"),
};

export const petApi = {
  list: () => apiGet("/pets"),
  create: (body) => apiPost("/pets", body),
  update: (id, body) => apiPut(`/pets/${id}`, body),
  remove: (id) => apiDelete(`/pets/${id}`),
};

export const petPlanApi = {
  list: () => apiGet("/pet-care-plans"),
  create: (body) => apiPost("/pet-care-plans", body),
  update: (id, body) => apiPut(`/pet-care-plans/${id}`, body),
  updateCompleted: (id, completed) => apiPatch(`/pet-care-plans/${id}/completed`, { completed }),
  remove: (id) => apiDelete(`/pet-care-plans/${id}`),
};

export const videoApi = {
  list: (params = {}) => apiGet("/emergency-videos", { params }),
  bookmarks: (params = {}) => apiGet("/emergency-videos/bookmarks", { params }),
  find: (id) => apiGet(`/emergency-videos/${id}`),
  create: (body) => apiPost("/emergency-videos", body),
  update: (id, body) => apiPut(`/emergency-videos/${id}`, body),
  remove: (id) => apiDelete(`/emergency-videos/${id}`),
  bookmark: (id) => apiPost(`/emergency-videos/${id}/bookmark`, {}),
  unbookmark: (id) => apiDelete(`/emergency-videos/${id}/bookmark`),
  reviews: (videoId) => apiGet(`/emergency-videos/${videoId}/reviews`),
  createReview: (videoId, body) => apiPost(`/emergency-videos/${videoId}/reviews`, body),
  updateReview: (reviewId, body) => apiPut(`/video-reviews/${reviewId}`, body),
  removeReview: (reviewId) => apiDelete(`/video-reviews/${reviewId}`),
};

export const emergencyApi = {
  create: (body) => apiPost("/emergency-checks", body),
  list: () => apiGet("/emergency-checks"),
  report: (id) => apiGet(`/emergency-checks/${id}/vet-report`),
  remove: (id) => apiDelete(`/emergency-checks/${id}`),
  downloadReportPdf: (id) => apiDownload(`/emergency-checks/${id}/vet-report/pdf`, "vet-report.pdf"),
};

export const caseBoardApi = {
  list: (params = {}) => apiGet("/case-posts", { params }),
  find: (id) => apiGet(`/case-posts/${id}`),
  create: (body) => apiPost("/case-posts", body),
  update: (id, body) => apiPut(`/case-posts/${id}`, body),
  remove: (id) => apiDelete(`/case-posts/${id}`),
  comments: (postId) => apiGet(`/case-posts/${postId}/comments`),
  createComment: (postId, body) => apiPost(`/case-posts/${postId}/comments`, body),
  updateComment: (commentId, body) => apiPut(`/case-comments/${commentId}`, body),
  removeComment: (commentId) => apiDelete(`/case-comments/${commentId}`),
};

export const hospitalApi = {
  list: (params = {}) => apiGet("/hospitals", { params }),
  find: (id) => apiGet(`/hospitals/${id}`),
  regions: () => apiGet("/hospitals/regions"),
  syncHours: (params = {}) => {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") query.set(key, value);
    });
    const suffix = query.toString() ? `?${query.toString()}` : "";
    return apiPost(`/hospitals/sync-hours${suffix}`, {});
  },
};

export const foodApi = {
  list: (keyword) => apiGet("/food-safety", { params: { keyword } }),
};
