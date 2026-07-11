import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../api/admin';
import { useAuthStore } from '../store/auth';

export function AdminLoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { setToken } = useAuthStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      const token = await adminApi.login({ username, password });
      setToken(token);
      navigate('/admin');
    } catch (e: any) {
      setError(e.message || '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-stone-50 px-4">
      <div className="w-full max-w-md bg-white rounded-2xl p-8 md:p-10 border border-stone-100">
        <h1 className="text-2xl font-medium text-stone-900 text-center mb-2">管理后台</h1>
        <p className="text-sm text-stone-500 text-center mb-8">仅管理员可登录</p>

        {error && <p className="text-sm text-red-500 mb-4 text-center">{error}</p>}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-stone-600 mb-1">用户名</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-4 py-2.5 border border-stone-200 rounded-lg text-sm focus:outline-none focus:border-stone-400"
              placeholder="admin"
              required
            />
          </div>
          <div>
            <label className="block text-sm text-stone-600 mb-1">密码</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-4 py-2.5 border border-stone-200 rounded-lg text-sm focus:outline-none focus:border-stone-400"
              placeholder="123456"
              required
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-stone-900 text-white py-3 rounded-full text-sm font-medium hover:bg-stone-800 transition-colors disabled:opacity-60"
          >
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  );
}
