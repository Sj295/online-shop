import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { SectionHeader } from '../components/SectionHeader';
import { cartApi, orderApi, addressApi } from '../api';
import { useCartStore } from '../store/cart';
import type { CartItem, Address } from '../types';
import { Minus, Plus, Trash2, ShoppingBag } from 'lucide-react';

export function CartPage() {
  const [items, setItems] = useState<CartItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<number | undefined>(undefined);
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();
  const setCartItems = useCartStore((s) => s.setItems);

  useEffect(() => {
    loadCart();
    addressApi.list().then((res) => {
      setAddresses(res);
      const def = res.find((a) => a.isDefault === 1);
      if (def) setSelectedAddress(def.id);
    });
  }, []);

  const loadCart = async () => {
    try {
      const res = await cartApi.list();
      setItems(res);
      setCartItems(res);
    } catch (e: any) {
      if (e.message?.includes('登录') || e.message?.includes('unauthorized')) {
        navigate('/login');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedItems = items.filter((i) => i.selected === 1);
  const total = selectedItems.reduce((sum, i) => sum + i.price * i.quantity, 0);

  const updateQuantity = async (item: CartItem, delta: number) => {
    const quantity = Math.max(0, item.quantity + delta);
    await cartApi.update({ cartItemId: item.id, quantity });
    loadCart();
  };

  const toggleSelect = async (item: CartItem) => {
    await cartApi.update({ cartItemId: item.id, selected: item.selected === 1 ? 0 : 1 });
    loadCart();
  };

  const removeItem = async (id: number) => {
    await cartApi.delete(id);
    loadCart();
  };

  const handleCheckout = async () => {
    if (selectedItems.length === 0) {
      alert('请选择商品');
      return;
    }
    if (!selectedAddress) {
      alert('请选择收货地址');
      return;
    }
    setSubmitting(true);
    try {
      const { orderNo } = await orderApi.create({ addressId: selectedAddress });
      let finalStatus = '';
      for (let i = 0; i < 60; i++) {
        const statusRes = await orderApi.createStatus(orderNo);
        finalStatus = statusRes.status;
        if (finalStatus === 'SUCCESS') {
          break;
        }
        if (finalStatus === 'FAILED') {
          throw new Error(statusRes.message || '订单创建失败');
        }
        await new Promise((resolve) => setTimeout(resolve, 500));
      }
      if (finalStatus !== 'SUCCESS') {
        throw new Error('订单创建超时');
      }
      await orderApi.pay(orderNo);
      alert('订单创建并支付成功');
      loadCart();
      navigate('/orders');
    } catch (e: any) {
      alert(e.message || '下单失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="container-main py-32 text-center text-stone-400">加载中...</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="container-main py-10 md:py-14">
        <SectionHeader title="购物车" />

        {items.length === 0 ? (
          <div className="py-32 text-center">
            <ShoppingBag className="w-12 h-12 mx-auto text-stone-300 mb-4" />
            <p className="text-stone-500 text-sm">购物车是空的</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 md:gap-12">
            <div className="lg:col-span-2 space-y-4">
              {items.map((item) => (
                <div key={item.id} className="flex gap-4 p-4 bg-white rounded-xl border border-stone-100">
                  <button
                    onClick={() => toggleSelect(item)}
                    className={`w-5 h-5 rounded-full border flex items-center justify-center ${
                      item.selected === 1 ? 'bg-stone-900 border-stone-900 text-white' : 'border-stone-300'
                    }`}
                  >
                    {item.selected === 1 && '✓'}
                  </button>
                  <img src={item.productImage} alt={item.productName} className="w-24 h-32 object-cover rounded-lg bg-stone-100" />
                  <div className="flex-1 flex flex-col">
                    <h3 className="text-sm font-medium text-stone-900">{item.productName}</h3>
                    <p className="text-xs text-stone-500 mt-1">{item.subtitle}</p>
                    <div className="mt-auto flex items-center justify-between">
                      <span className="text-sm font-medium">¥{item.price.toFixed(2)}</span>
                      <div className="flex items-center border border-stone-200 rounded-lg">
                        <button onClick={() => updateQuantity(item, -1)} className="p-1.5 hover:bg-stone-100">
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="w-8 text-center text-sm">{item.quantity}</span>
                        <button onClick={() => updateQuantity(item, 1)} className="p-1.5 hover:bg-stone-100">
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  </div>
                  <button onClick={() => removeItem(item.id)} className="text-stone-400 hover:text-red-500">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>

            <div className="space-y-4">
              <div className="p-5 bg-white rounded-xl border border-stone-100">
                <h3 className="text-sm font-medium text-stone-900 mb-3">收货地址</h3>
                {addresses.length === 0 ? (
                  <p className="text-sm text-stone-500">暂无地址，请先添加</p>
                ) : (
                  <div className="space-y-2">
                    {addresses.map((addr) => (
                      <label key={addr.id} className="flex items-start gap-2 text-sm cursor-pointer">
                        <input
                          type="radio"
                          name="address"
                          checked={selectedAddress === addr.id}
                          onChange={() => setSelectedAddress(addr.id)}
                          className="mt-1"
                        />
                        <div className="text-stone-600">
                          <p>{addr.receiverName} {addr.phone}</p>
                          <p>{addr.province}{addr.city}{addr.district}{addr.detail}</p>
                        </div>
                      </label>
                    ))}
                  </div>
                )}
              </div>

              <div className="p-5 bg-white rounded-xl border border-stone-100">
                <div className="flex justify-between text-sm mb-2">
                  <span className="text-stone-500">商品小计</span>
                  <span>¥{total.toFixed(2)}</span>
                </div>
                <div className="flex justify-between text-sm mb-4">
                  <span className="text-stone-500">运费</span>
                  <span>¥0.00</span>
                </div>
                <div className="flex justify-between text-base font-medium pt-4 border-t border-stone-100">
                  <span>合计</span>
                  <span>¥{total.toFixed(2)}</span>
                </div>
                <button
                  onClick={handleCheckout}
                  disabled={submitting}
                  className="w-full mt-5 bg-stone-900 text-white py-3 rounded-full text-sm font-medium hover:bg-stone-800 transition-colors disabled:opacity-60"
                >
                  {submitting ? '处理中...' : '结算并支付'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
}
