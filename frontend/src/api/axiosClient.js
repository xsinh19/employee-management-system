import axios from 'axios';

const TOKEN_KEY = 'ems_token';
const USER_KEY = 'ems_user';

export const tokenStorage = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  getUser: () => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  save: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

// Relative base URL: in dev the Vite proxy forwards /api to Spring Boot.
const axiosClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

// Attach the JWT to every outgoing request.
axiosClient.interceptors.request.use((config) => {
  const token = tokenStorage.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// A 401 on any call other than login means the token is missing, expired or invalid:
// clear it and send the user back to the login page.
axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginCall = error.config?.url?.includes('/auth/login');
    if (error.response?.status === 401 && !isLoginCall) {
      tokenStorage.clear();
      window.location.assign('/login?expired=1');
    }
    return Promise.reject(error);
  },
);

/** Pulls a human-readable message out of the backend's ErrorResponse JSON. */
export function getErrorMessage(error, fallback = 'Something went wrong') {
  if (!error.response) return 'Cannot reach the server. Is the backend running?';
  return error.response.data?.message || fallback;
}

export default axiosClient;
