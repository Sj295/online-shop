import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Layout } from '../components/Layout';
import { ProductGrid } from '../components/ProductGrid';
import { SectionHeader } from '../components/SectionHeader';
import { homeApi } from '../api';
import type { HomeData } from '../types';
import { ArrowRight } from 'lucide-react';

export function HomePage() {
  const [data, setData] = useState<HomeData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    homeApi
      .getHome()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <Layout>
        <div className="container-main py-32 text-center text-stone-400 text-sm">加载中...</div>
      </Layout>
    );
  }

  if (error || !data) {
    return (
      <Layout>
        <div className="container-main py-32 text-center text-red-500 text-sm">{error || '加载失败'}</div>
      </Layout>
    );
  }

  const currentCarousel = data.carousels[0];

  return (
    <Layout>
      <section className="container-main py-8 md:py-12">
        <div className="relative aspect-[16/7] md:aspect-[21/8] overflow-hidden rounded-2xl bg-stone-200">
          {currentCarousel && (
            <>
              <img
                src={currentCarousel.image}
                alt={currentCarousel.title}
                className="h-full w-full object-cover"
              />
              <div className="absolute inset-0 bg-gradient-to-r from-stone-900/40 to-transparent flex items-center">
                <div className="container-main">
                  <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.8 }}
                    className="max-w-md text-white"
                  >
                    <p className="text-xs font-medium tracking-widest uppercase mb-3 opacity-90">
                      New Collection
                    </p>
                    <h2 className="text-3xl md:text-5xl font-medium tracking-tight mb-4">
                      {currentCarousel.title}
                    </h2>
                    <Link
                      to={currentCarousel.link || '/category'}
                      className="inline-flex items-center gap-2 text-sm font-medium bg-white text-stone-900 px-5 py-2.5 rounded-full hover:bg-stone-100 transition-colors"
                    >
                      立即选购
                      <ArrowRight className="w-4 h-4" />
                    </Link>
                  </motion.div>
                </div>
              </div>
            </>
          )}
        </div>
      </section>

      <section className="container-main py-10 md:py-14">
        <SectionHeader title="热门分类" subtitle="按生活方式探索精选商品" />
        <div className="grid grid-cols-2 md:grid-cols-5 gap-3 md:gap-4">
          {data.categories.map((category) => (
            <Link
              key={category.id}
              to={`/category?cid=${category.id}`}
              className="group relative aspect-[3/2] overflow-hidden rounded-xl bg-stone-200"
            >
              <div className="absolute inset-0 flex items-center justify-center bg-stone-100 group-hover:bg-stone-200 transition-colors">
                <span className="text-sm font-medium text-stone-700">{category.name}</span>
              </div>
            </Link>
          ))}
        </div>
      </section>

      <section className="container-main py-10 md:py-14">
        <SectionHeader
          title="热销推荐"
          subtitle="大家都在买的高口碑好物"
          action={
            <Link to="/category" className="text-sm text-stone-500 hover:text-stone-900 flex items-center gap-1">
              查看全部 <ArrowRight className="w-4 h-4" />
            </Link>
          }
        />
        <ProductGrid products={data.hotProducts} />
      </section>

      <section className="container-main py-10 md:py-14 pb-16 md:pb-24">
        <SectionHeader
          title="新品上市"
          subtitle="近期上架的新鲜选择"
          action={
            <Link to="/category" className="text-sm text-stone-500 hover:text-stone-900 flex items-center gap-1">
              查看全部 <ArrowRight className="w-4 h-4" />
            </Link>
          }
        />
        <ProductGrid products={data.newProducts} />
      </section>
    </Layout>
  );
}
