import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { ProductGrid } from '../components/ProductGrid';
import { SectionHeader } from '../components/SectionHeader';
import { categoryApi, productApi } from '../api';
import type { Category, Product, Page } from '../types';

export function CategoryPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [categories, setCategories] = useState<Category[]>([]);
  const [products, setProducts] = useState<Page<Product>>({ records: [], total: 0, size: 12, current: 1, pages: 0 });
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeId, setActiveId] = useState<number | undefined>(
    searchParams.get('cid') ? Number(searchParams.get('cid')) : undefined
  );

  useEffect(() => {
    categoryApi.list().then(setCategories).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    productApi
      .list({ categoryId: activeId || 0, page, size: 12 })
      .then((res) => {
        setProducts(res);
        setError('');
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [activeId, page]);

  const handleCategory = (id: number | undefined) => {
    setActiveId(id);
    setPage(1);
    navigate(id ? `/category?cid=${id}` : '/category');
  };

  const handlePage = (p: number) => {
    setPage(p);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <Layout>
      <div className="container-main py-10 md:py-14">
        <SectionHeader title="全部商品" subtitle="精选生活好物，质感日常" />

        <div className="flex flex-wrap gap-2 mb-8 md:mb-10">
          <button
            onClick={() => handleCategory(undefined)}
            className={`px-4 py-2 text-sm rounded-full border transition-colors ${
              activeId === undefined
                ? 'bg-stone-900 text-white border-stone-900'
                : 'bg-white text-stone-600 border-stone-200 hover:border-stone-400'
            }`}
          >
            全部
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => handleCategory(cat.id)}
              className={`px-4 py-2 text-sm rounded-full border transition-colors ${
                activeId === cat.id
                  ? 'bg-stone-900 text-white border-stone-900'
                  : 'bg-white text-stone-600 border-stone-200 hover:border-stone-400'
              }`}
            >
              {cat.name}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="py-32 text-center text-stone-400 text-sm">加载中...</div>
        ) : error ? (
          <div className="py-32 text-center text-red-500 text-sm">{error}</div>
        ) : (
          <ProductGrid products={products.records} />
        )}

        {products.pages > 1 && (
          <div className="flex justify-center gap-2 mt-12">
            {Array.from({ length: products.pages }).map((_, i) => (
              <button
                key={i}
                onClick={() => handlePage(i + 1)}
                className={`w-8 h-8 text-sm rounded-full ${
                  page === i + 1 ? 'bg-stone-900 text-white' : 'bg-white text-stone-600 border border-stone-200'
                }`}
              >
                {i + 1}
              </button>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}
