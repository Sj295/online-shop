import { get, post, put, del } from './request';
import type { HomeData, Product, Page, Category, CartItem, Address, Order, OrderCreateResponse, OrderCreateStatusResponse, User } from '../types';

export const homeApi = {
  getHome: () => get<HomeData>('/api/home/index'),
};

export const categoryApi = {
  list: () => get<Category[]>('/api/category/list'),
};

export const productApi = {
  detail: (id: number) => get<Product>(`/api/product/${id}`),
  list: (params: { categoryId?: number; page?: number; size?: number }) => get<Page<Product>>('/api/product/list', params),
  search: (params: { keyword?: string; page?: number; size?: number }) => get<Page<Product>>('/api/product/search', params),
};

export const authApi = {
  login: (data: { username: string; password: string }) => post<string>('/api/user/login', data),
  register: (data: { username: string; password: string; nickname?: string }) => post<string>('/api/user/register', data),
  info: () => get<User>('/api/user/info'),
  logout: () => post<void>('/api/user/logout'),
};

export const cartApi = {
  list: () => get<CartItem[]>('/api/cart/list'),
  add: (data: { productId: number; skuId?: number; quantity: number }) => post<void>('/api/cart/add', data),
  update: (data: { cartItemId: number; quantity?: number; selected?: number }) => put<void>('/api/cart/update', data),
  delete: (id: number) => del<void>(`/api/cart/${id}`),
};

export const addressApi = {
  list: () => get<Address[]>('/api/address/list'),
  default: () => get<Address>('/api/address/default'),
  add: (data: Omit<Address, 'id'>) => post<void>('/api/address/add', data),
};

export const orderApi = {
  create: (data: { addressId: number; remark?: string }) => post<OrderCreateResponse>('/api/order/create', data),
  createStatus: (orderNo: string) => get<OrderCreateStatusResponse>('/api/order/create/status', { orderNo }),
  list: (status?: number) => get<Order[]>('/api/order/list', { status }),
  detail: (orderNo: string) => get<Order>('/api/order/detail', { orderNo }),
  pay: (orderNo: string) => post<void>(`/api/order/pay?orderNo=${encodeURIComponent(orderNo)}`),
  cancel: (orderNo: string) => post<void>(`/api/order/cancel?orderNo=${encodeURIComponent(orderNo)}`),
};
