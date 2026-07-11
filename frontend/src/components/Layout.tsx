import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ShoppingBag, User, Search, Menu, X } from 'lucide-react';
import { useAuthStore } from '../store/auth';
import { useCartStore } from '../store/cart';
import { useState } from 'react';
import { authApi } from '../api';

export function Layout({ children }: { children: React.ReactNode }) {
  const { isLoggedIn, logout, user } = useAuthStore();
  const cartCount = useCartStore((s) => s.selectedCount());
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const navLinks = [
    { label: '首页', href: '/' },
    { label: '分类', href: '/category' },
    { label: '购物车', href: '/cart' },
    { label: '订单', href: '/orders' },
  ];

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {}
    logout();
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col bg-stone-50 text-stone-800 font-sans">
      <header className="sticky top-0 z-50 bg-stone-50/90 backdrop-blur-md border-b border-stone-200/60">
        <div className="container-main py-4 md:py-5">
          <div className="flex items-center justify-between">
            <Link to="/" className="flex items-center gap-2 group">
              <span className="text-lg md:text-xl font-medium tracking-tight text-stone-900">
                BOUTIQUE
              </span>
            </Link>

            <nav className="hidden md:flex items-center gap-8">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  to={link.href}
                  className={`text-sm tracking-wide transition-colors hover:text-stone-900 ${
                    location.pathname === link.href ? 'text-stone-900 font-medium' : 'text-stone-500'
                  }`}
                >
                  {link.label}
                </Link>
              ))}
            </nav>

            <div className="flex items-center gap-4 md:gap-6">
              <button
                onClick={() => setSearchOpen(!searchOpen)}
                className="p-2 text-stone-500 hover:text-stone-900 transition-colors"
                aria-label="搜索"
              >
                <Search className="w-5 h-5" />
              </button>

              <Link to="/cart" className="relative p-2 text-stone-500 hover:text-stone-900 transition-colors">
                <ShoppingBag className="w-5 h-5" />
                {cartCount > 0 && (
                  <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-stone-800 text-white text-[10px] font-medium rounded-full flex items-center justify-center">
                    {cartCount}
                  </span>
                )}
              </Link>

              {isLoggedIn() ? (
                <div className="hidden md:flex items-center gap-4">
                  <Link to="/orders" className="flex items-center gap-2 text-sm text-stone-500 hover:text-stone-900">
                    <User className="w-4 h-4" />
                    {user?.nickname || user?.username}
                  </Link>
                  <button onClick={handleLogout} className="text-sm text-stone-500 hover:text-stone-900">
                    退出
                  </button>
                </div>
              ) : (
                <Link to="/login" className="hidden md:block text-sm text-stone-500 hover:text-stone-900">
                  登录
                </Link>
              )}

              <button
                className="md:hidden p-2 text-stone-500 hover:text-stone-900"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              >
                {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {searchOpen && (
            <div className="mt-4 pb-2">
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  const keyword = (e.currentTarget.keyword as HTMLInputElement).value.trim();
                  if (keyword) navigate(`/search?keyword=${encodeURIComponent(keyword)}`);
                  setSearchOpen(false);
                }}
                className="flex items-center gap-2 border-b border-stone-300 pb-2"
              >
                <input
                  name="keyword"
                  type="text"
                  placeholder="搜索商品..."
                  className="flex-1 bg-transparent outline-none text-sm text-stone-800 placeholder:text-stone-400"
                />
                <button type="submit" className="text-sm text-stone-600 hover:text-stone-900">搜索</button>
              </form>
            </div>
          )}
        </div>

        {mobileMenuOpen && (
          <div className="md:hidden border-t border-stone-200/60 bg-stone-50">
            <div className="container-main py-4 flex flex-col gap-4">
              {navLinks.map((link) => (
                <Link
                  key={link.href}
                  to={link.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-sm text-stone-600 hover:text-stone-900"
                >
                  {link.label}
                </Link>
              ))}
              {!isLoggedIn() && (
                <Link to="/login" onClick={() => setMobileMenuOpen(false)} className="text-sm text-stone-600">
                  登录
                </Link>
              )}
            </div>
          </div>
        )}
      </header>

      <main className="flex-1">{children}</main>

      <footer className="border-t border-stone-200/60 bg-stone-100">
        <div className="container-main py-12 md:py-16">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 md:gap-12">
            <div>
              <h4 className="text-sm font-medium text-stone-900 mb-4">BOUTIQUE</h4>
              <p className="text-sm text-stone-500 leading-relaxed">
                精选生活方式好物，以极简设计与品质细节，为日常注入质感。
              </p>
            </div>
            <div>
              <h4 className="text-sm font-medium text-stone-900 mb-4">探索</h4>
              <ul className="space-y-2 text-sm text-stone-500">
                <li><Link to="/" className="hover:text-stone-900">首页</Link></li>
                <li><Link to="/category" className="hover:text-stone-900">分类</Link></li>
                <li><Link to="/cart" className="hover:text-stone-900">购物车</Link></li>
              </ul>
            </div>
            <div>
              <h4 className="text-sm font-medium text-stone-900 mb-4">联系</h4>
              <ul className="space-y-2 text-sm text-stone-500">
                <li>help@boutique.local</li>
                <li>400-000-0000</li>
              </ul>
            </div>
          </div>
          <div className="mt-12 pt-8 border-t border-stone-200/60 text-xs text-stone-400 text-center">
            © 2026 BOUTIQUE. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
}
