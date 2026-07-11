import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import type { Product } from '../types';

interface ProductCardProps {
  product: Product;
  index?: number;
}

export function ProductCard({ product, index = 0 }: ProductCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: index * 0.05, ease: [0.22, 1, 0.36, 1] }}
    >
      <Link to={`/product/${product.id}`} className="group block">
        <div className="relative aspect-[4/5] overflow-hidden rounded-xl bg-stone-100 mb-4">
          <img
            src={product.mainImage}
            alt={product.name}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-700 ease-out group-hover:scale-105"
          />
          {product.originalPrice > product.price && (
            <span className="absolute top-3 left-3 bg-stone-900 text-white text-[10px] font-medium px-2 py-1 rounded-full">
              SALE
            </span>
          )}
        </div>
        <div className="space-y-1">
          <h3 className="text-sm font-medium text-stone-900 group-hover:text-stone-600 transition-colors">
            {product.name}
          </h3>
          <p className="text-xs text-stone-500 line-clamp-1">{product.subtitle}</p>
          <div className="flex items-center gap-2 pt-1">
            <span className="text-sm font-medium text-stone-900">
              ¥{product.price.toFixed(2)}
            </span>
            {product.originalPrice > 0 && (
              <span className="text-xs text-stone-400 line-through">
                ¥{product.originalPrice.toFixed(2)}
              </span>
            )}
          </div>
        </div>
      </Link>
    </motion.div>
  );
}
