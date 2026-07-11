export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

export interface User {
  id: number;
  username: string;
  nickname: string;
  phone: string;
  email: string;
  avatar: string;
  createTime: string;
}

export interface Category {
  id: number;
  name: string;
  icon: string;
}

export interface Product {
  id: number;
  categoryId: number;
  name: string;
  subtitle: string;
  description: string;
  mainImage: string;
  price: number;
  originalPrice: number;
  stock: number;
  saleCount: number;
  isHot: number;
  isNew: number;
}

export interface CartItem {
  id: number;
  productId: number;
  skuId: number;
  quantity: number;
  selected: number;
  productName: string;
  subtitle: string;
  productImage: string;
  price: number;
}

export interface Address {
  id: number;
  receiverName: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  detail: string;
  isDefault: number;
}

export interface Order {
  id: number;
  orderNo: string;
  totalAmount: number;
  payAmount: number;
  freightAmount: number;
  status: number;
  statusText: string;
  receiverName: string;
  receiverPhone: string;
  receiverAddress: string;
  createTime: string;
  items?: OrderItem[];
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productImage: string;
  price: number;
  quantity: number;
  totalAmount: number;
}

export interface Carousel {
  id: number;
  title: string;
  image: string;
  link: string;
}

export interface HomeData {
  carousels: Carousel[];
  categories: Category[];
  hotProducts: Product[];
  newProducts: Product[];
}

export interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
