import { create } from 'zustand';
import type { CartItem } from '../types';

interface CartState {
  items: CartItem[];
  setItems: (items: CartItem[]) => void;
  selectedCount: () => number;
  selectedTotal: () => number;
}

export const useCartStore = create<CartState>()((set, get) => ({
  items: [],
  setItems: (items) => set({ items }),
  selectedCount: () => get().items.filter((i) => i.selected === 1).reduce((sum, i) => sum + i.quantity, 0),
  selectedTotal: () => get().items.filter((i) => i.selected === 1).reduce((sum, i) => sum + i.price * i.quantity, 0),
}));
