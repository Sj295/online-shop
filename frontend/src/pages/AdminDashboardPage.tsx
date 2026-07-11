import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { adminApi } from '../api/admin';
import { useAuthStore } from '../store/auth';

export function AdminDashboardPage() {
  const [activeTab, setActiveTab] = useState<'product' | 'order'>('product');
  const [products, setProducts] = useState<any[]>([]);
  const [orders, setOrders] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const { logout } = useAuthStore();

  useEffect(() => {
    if (activeTab === 'product') {
      setLoading(true);
      adminApi.productList({ page: 1, size: 20 }).then((res) => setProducts(res.records)).finally(() => setLoading(false));
    } else {
      setLoading(true);
      adminApi.orderList({ page: 1, size: 20 }).then((res) => setOrders(res.records)).finally(() => setLoading(false));
    }
  }, [activeTab]);

  const toggleProductStatus = async (id: number, current: number) => {
    await adminApi.productStatus(id, current === 1 ? 0 : 1);
    const res = await adminApi.productList({ page: 1, size: 20 });
    setProducts(res.records);
  };

  const shipOrder = async (orderNo: string) => {
    await adminApi.orderShip(orderNo);
    const res = await adminApi.orderList({ page: 1, size: 20 });
    setOrders(res.records);
  };

  const handleLogout = () => {
    logout();
    navigate('/admin/login');
  };

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="bg-white border-b border-stone-200">
        <div className="container-main py-4 flex items-center justify-between">
          <h1 className="text-lg font-medium text-stone-900">BOUTIQUE 管理后台</h1>
          <button onClick={handleLogout} className="text-sm text-stone-500 hover:text-stone-900">退出</button>
        </div>
      </header>

      <main className="container-main py-8">
        <div className="flex gap-4 mb-8">
          <button
            onClick={() => setActiveTab('product')}
            className={`px-4 py-2 text-sm rounded-full border ${
              activeTab === 'product'
                ? 'bg-stone-900 text-white border-stone-900'
                : 'bg-white text-stone-600 border-stone-200'
            }`}
          >
            商品管理
          </button>
          <button
            onClick={() => setActiveTab('order')}
            className={`px-4 py-2 text-sm rounded-full border ${
              activeTab === 'order'
                ? 'bg-stone-900 text-white border-stone-900'
                : 'bg-white text-stone-600 border-stone-200'
            }`}
          >
            订单管理
          </button>
        </div>

        {loading ? (
          <div className="text-center text-stone-400 py-20">加载中...</div>
        ) : activeTab === 'product' ? (
          <div className="bg-white rounded-xl border border-stone-100 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-stone-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">ID</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">商品名称</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">价格</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">库存</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">状态</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {products.map((p) => (
                  <tr key={p.id}>
                    <td className="px-4 py-3">{p.id}</td>
                    <td className="px-4 py-3">{p.name}</td>
                    <td className="px-4 py-3">¥{p.price.toFixed(2)}</td>
                    <td className="px-4 py-3">{p.stock}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-1 rounded-full ${p.status === 1 ? 'bg-green-100 text-green-700' : 'bg-stone-100 text-stone-600'}`}>
                        {p.status === 1 ? '上架' : '下架'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => toggleProductStatus(p.id, p.status)}
                        className="text-xs text-stone-600 hover:text-stone-900 underline"
                      >
                        {p.status === 1 ? '下架' : '上架'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-stone-100 overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-stone-50">
                <tr>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">订单号</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">金额</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">状态</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">下单时间</th>
                  <th className="px-4 py-3 text-left font-medium text-stone-600">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-stone-100">
                {orders.map((o) => (
                  <tr key={o.orderNo}>
                    <td className="px-4 py-3 font-mono text-xs">{o.orderNo}</td>
                    <td className="px-4 py-3">¥{o.payAmount.toFixed(2)}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs px-2 py-1 rounded-full ${o.status === 1 ? 'bg-green-100 text-green-700' : 'bg-stone-100 text-stone-600'}`}>
                        {o.status === 0 ? '待付款' : o.status === 1 ? '已付款' : o.status === 2 ? '已发货' : '其他'}
                      </span>
                    </td>
                    <td className="px-4 py-3">{o.createTime}</td>
                    <td className="px-4 py-3">
                      {o.status === 1 && (
                        <button
                          onClick={() => shipOrder(o.orderNo)}
                          className="text-xs text-stone-600 hover:text-stone-900 underline"
                        >
                          发货
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  );
}
