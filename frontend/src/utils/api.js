const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const TOKEN_STORAGE_KEY = 'authToken';
const TOKEN_EXPIRES_AT_KEY = 'authTokenExpiresAt';

const getToken = () => localStorage.getItem(TOKEN_STORAGE_KEY);

const saveAuth = ({ token, expiresAt }) => {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  localStorage.setItem(TOKEN_EXPIRES_AT_KEY, expiresAt);
};

const clearAuth = () => {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(TOKEN_EXPIRES_AT_KEY);
};

const parseErrorMessage = async (response) => {
  try {
    const data = await response.json();
    return data.message || '요청 처리 중 오류가 발생했습니다.';
  } catch (err) {
    return '요청 처리 중 오류가 발생했습니다.';
  }
};

const request = async (path, { method = 'GET', body, auth = true } = {}) => {
  const headers = {};

  if (body) {
    headers['Content-Type'] = 'application/json';
  }

  if (auth) {
    const token = getToken();
    if (!token) {
      const err = new Error('로그인이 필요합니다.');
      err.status = 401;
      throw err;
    }

    headers.Authorization = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch (err) {
    const networkError = new Error('백엔드 서버에 연결할 수 없습니다. Spring Boot 서버가 실행 중인지 확인해주세요.');
    networkError.status = 0;
    throw networkError;
  }

  if (!response.ok) {
    const message = await parseErrorMessage(response);
    const err = new Error(message);
    err.status = response.status;

    if (response.status === 401) {
      clearAuth();
      localStorage.removeItem('currentUser');
    }

    throw err;
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
};

export const api = {
  hasAuth() {
    const token = getToken();
    const expiresAt = localStorage.getItem(TOKEN_EXPIRES_AT_KEY);

    if (!token || !expiresAt) return false;
    if (new Date(expiresAt).getTime() <= Date.now()) {
      clearAuth();
      localStorage.removeItem('currentUser');
      return false;
    }

    return true;
  },

  logout() {
    clearAuth();
    localStorage.removeItem('currentUser');
  },

  async getTestUsers() {
    return request('/auth/test-users', { auth: false });
  },

  async login(id, password) {
    const data = await request('/auth/login', {
      method: 'POST',
      body: { id, password },
      auth: false,
    });

    saveAuth(data);
    return data.user;
  },

  async getUsers() {
    return request('/users');
  },

  async getFranchises() {
    return request('/franchises');
  },

  async getAverages() {
    return request('/averages');
  },

  async getAlerts() {
    return request('/alerts');
  },

  async getAiInsights(franchiseId) {
    return request(`/franchises/${encodeURIComponent(franchiseId)}/ai-insights`);
  },

  async getLatestAiInsight(franchiseId) {
    return request(`/franchises/${encodeURIComponent(franchiseId)}/ai-insights/latest`);
  },

  async saveAiInsight(franchiseId, payload) {
    return request(`/franchises/${encodeURIComponent(franchiseId)}/ai-insights`, {
      method: 'POST',
      body: payload,
    });
  },

  async generateAiInsight(franchiseId) {
    return request(`/franchises/${encodeURIComponent(franchiseId)}/ai-insights/generate`, {
      method: 'POST',
    });
  },

  async updateAiInsightNote(franchiseId, insightId, note) {
    return request(`/franchises/${encodeURIComponent(franchiseId)}/ai-insights/${encodeURIComponent(insightId)}/note`, {
      method: 'POST',
      body: { note },
    });
  },

  async getAdminUsers() {
    return request('/admin/users');
  },

  async createFranchise(payload) {
    return request('/admin/franchises', {
      method: 'POST',
      body: payload,
    });
  },

  async closeFranchise(franchiseId, closureNote = '') {
    return request(`/admin/franchises/${encodeURIComponent(franchiseId)}/close`, {
      method: 'POST',
      body: { closureNote },
    });
  },

  async assignManager(franchiseId, managerId) {
    return request('/admin/assign-manager', {
      method: 'POST',
      body: { franchiseId, managerId },
    });
  },

  async updateFranchiseLocation(franchiseId, payload) {
    return request(`/admin/franchises/${encodeURIComponent(franchiseId)}/location`, {
      method: 'POST',
      body: payload,
    });
  },

  async toggleAi(userId, canUseAI) {
    return request('/admin/toggle-ai', {
      method: 'POST',
      body: { userId, canUseAI },
    });
  },
};
