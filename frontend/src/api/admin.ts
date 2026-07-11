import axios from 'axios';

const adminRequest = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 10000,
});

adminRequest.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = token;
  }
  return config;
});

adminRequest.interceptors.response.use(
  (response) => {
    const data = response.data as { code: number; message: string; data: unknown };
    if (data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'));
    }
    return response;
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络错误';
    return Promise.reject(new Error(message));
  }
);

export async function adminGet<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await adminRequest.get<{ code: number; message: string; data: T }>(url, { params });
  return res.data.data;
}

export async function adminPost<T>(url: string, data?: unknown): Promise<T> {
  const res = await adminRequest.post<{ code: number; message: string; data: T }>(url, data);
  return res.data.data;
}

export async function adminPut<T>(url: string, data?: unknown): Promise<T> {
  const res = await adminRequest.put<{ code: number; message: string; data: T }>(url, data);
  return res.data.data;
}

export async function adminDel<T>(url: string): Promise<T> {
  const res = await adminRequest.delete<{ code: number; message: string; data: T }>(url);
  return res.data.data;
}

export const adminApi = {
  login: (data: { username: string; password: string }) => adminPost<string>('/api/admin/login', data),
  productList: (params: { page?: number; size?: number }) => adminGet<{ records: any[]; total: number }>('/api/admin/product/list', params),
  productStatus: (id: number, status: number) => adminPut<void>(`/api/admin/product/status/${id}?status=${status}`),
  orderList: (params: { status?: number; page?: number; size?: number }) => adminGet<{ records: any[]; total: number }>('/api/admin/order/list', params),
  orderShip: (orderNo: string) => adminPost<void>('/api/admin/order/ship', { orderNo }),
};
