import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Layout } from '../components/Layout';
import { ProductGrid } from '../components/ProductGrid';
import { SectionHeader } from '../components/SectionHeader';
import { productApi } from '../api';
import type { Product, Page } from '../types';

export function SearchPage() {
  const [searchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const [products, setProducts] = useState<Page<Product>>({ records: [], total: 0, size: 12, current: 1, pages: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    productApi
      .search({ keyword, page: 1, size: 12 })
      .then(setProducts)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [keyword]);

  return (
    <Layout>
      <div className="container-main py-10 md:py-14">
        <SectionHeader
          title={`搜索：${keyword}`}
          subtitle={`共 ${products.total} 件商品`}
        />
        {loading ? (
          <div className="py-32 text-center text-stone-400">加载中...</div>
        ) : error ? (
          <div className="py-32 text-center text-red-500">{error}</div>
        ) : (
          <ProductGrid products={products.records} />
        )}
      </div>
    </Layout>
  );
}
