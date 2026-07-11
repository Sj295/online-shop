import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { Result } from '../types';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 10000,
});

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = token;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

request.interceptors.response.use(
  (response) => {
    const data = response.data as Result<unknown>;
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return response;
  },
  (error: AxiosError) => {
    const message = (error.response?.data as Result<unknown>)?.message || error.message || '网络错误';
    return Promise.reject(new Error(message));
  }
);

export default request;

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await request.get<Result<T>>(url, { params });
  return res.data.data;
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  const res = await request.post<Result<T>>(url, data);
  return res.data.data;
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  const res = await request.put<Result<T>>(url, data);
  return res.data.data;
}

export async function del<T>(url: string): Promise<T> {
  const res = await request.delete<Result<T>>(url);
  return res.data.data;
}
