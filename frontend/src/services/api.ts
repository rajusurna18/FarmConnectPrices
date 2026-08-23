import axios from 'axios';
import { env } from '../config/env';
import { getCurrentIdToken } from './firebase/auth';

export const apiClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  async (config) => {
    try {
      const token = await getCurrentIdToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } catch (error) {
      console.warn('[apiClient] Failed to attach Firebase ID token:', error);
    }
    return config;
  },
  (error) => Promise.reject(error)
);
