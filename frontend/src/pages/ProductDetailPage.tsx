import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Layout } from '../components/Layout';
import { productApi, cartApi } from '../api';
import type { Product } from '../types';
import { Minus, Plus, ShoppingBag, ArrowLeft } from 'lucide-react';
import { useAuthStore } from '../store/auth';

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [product, setProduct] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [adding, setAdding] = useState(false);
  const { isLoggedIn } = useAuthStore();

  useEffect(() => {
    productApi
      .detail(Number(id))
      .then(setProduct)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  const handleAddToCart = async () => {
    if (!isLoggedIn()) {
      navigate('/login');
      return;
    }
    if (!product) return;
    setAdding(true);
    try {
      await cartApi.add({ productId: product.id, quantity });
      alert('已加入购物车');
    } catch (e: any) {
      alert(e.message || '添加失败');
    } finally {
      setAdding(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="container-main py-32 text-center text-stone-400">加载中...</div>
      </Layout>
    );
  }

  if (error || !product) {
    return (
      <Layout>
        <div className="container-main py-32 text-center text-red-500">{error || '商品不存在'}</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="container-main py-8 md:py-14">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1 text-sm text-stone-500 hover:text-stone-900 mb-6"
        >
          <ArrowLeft className="w-4 h-4" /> 返回
        </button>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 md:gap-12 lg:gap-16">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            className="aspect-[4/5] rounded-2xl bg-stone-100 overflow-hidden"
          >
            <img src={product.mainImage} alt={product.name} className="h-full w-full object-cover" />
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.1 }}
            className="flex flex-col"
          >
            <h1 className="text-2xl md:text-3xl font-medium text-stone-900 tracking-tight">{product.name}</h1>
            <p className="mt-3 text-stone-500 text-sm leading-relaxed">{product.subtitle}</p>

            <div className="mt-6 flex items-baseline gap-3">
              <span className="text-2xl font-medium text-stone-900">¥{product.price.toFixed(2)}</span>
              {product.originalPrice > 0 && (
                <span className="text-sm text-stone-400 line-through">¥{product.originalPrice.toFixed(2)}</span>
              )}
            </div>

            <div className="mt-8 pt-8 border-t border-stone-200/60">
              <p className="text-sm text-stone-600 leading-relaxed whitespace-pre-line">{product.description}</p>
            </div>

            <div className="mt-8 flex items-center gap-4">
              <span className="text-sm text-stone-500">数量</span>
              <div className="flex items-center border border-stone-200 rounded-lg">
                <button
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  className="p-2 hover:bg-stone-100"
                >
                  <Minus className="w-4 h-4" />
                </button>
                <span className="w-10 text-center text-sm">{quantity}</span>
                <button
                  onClick={() => setQuantity((q) => q + 1)}
                  className="p-2 hover:bg-stone-100"
                >
                  <Plus className="w-4 h-4" />
                </button>
              </div>
              <span className="text-xs text-stone-400">库存 {product.stock}</span>
            </div>

            <div className="mt-auto pt-8 flex gap-3">
              <button
                onClick={handleAddToCart}
                disabled={adding}
                className="flex-1 flex items-center justify-center gap-2 bg-stone-900 text-white px-6 py-3 rounded-full text-sm font-medium hover:bg-stone-800 transition-colors disabled:opacity-60"
              >
                <ShoppingBag className="w-4 h-4" />
                {adding ? '加入中...' : '加入购物车'}
              </button>
            </div>
          </motion.div>
        </div>
      </div>
    </Layout>
  );
}
