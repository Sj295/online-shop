import { useEffect, useState } from 'react';
import { Layout } from '../components/Layout';
import { SectionHeader } from '../components/SectionHeader';
import { orderApi } from '../api';
import type { Order } from '../types';

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [status, setStatus] = useState<number | undefined>(undefined);

  useEffect(() => {
    setLoading(true);
    orderApi
      .list(status)
      .then(setOrders)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [status]);

  const tabs = [
    { label: '全部', value: undefined },
    { label: '待付款', value: 0 },
    { label: '已付款', value: 1 },
    { label: '已发货', value: 2 },
    { label: '已完成', value: 3 },
  ];

  return (
    <Layout>
      <div className="container-main py-10 md:py-14">
        <SectionHeader title="我的订单" />

        <div className="flex flex-wrap gap-2 mb-8">
          {tabs.map((tab) => (
            <button
              key={tab.label}
              onClick={() => setStatus(tab.value)}
              className={`px-4 py-2 text-sm rounded-full border transition-colors ${
                status === tab.value
                  ? 'bg-stone-900 text-white border-stone-900'
                  : 'bg-white text-stone-600 border-stone-200 hover:border-stone-400'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {loading ? (
          <div className="py-32 text-center text-stone-400">加载中...</div>
        ) : error ? (
          <div className="py-32 text-center text-red-500">{error}</div>
        ) : orders.length === 0 ? (
          <div className="py-32 text-center text-stone-500">暂无订单</div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div key={order.orderNo} className="p-5 bg-white rounded-xl border border-stone-100">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <p className="text-xs text-stone-400">订单号：{order.orderNo}</p>
                    <p className="text-xs text-stone-400 mt-1">{order.createTime}</p>
                  </div>
                  <span className="text-sm font-medium text-stone-900">{order.statusText}</span>
                </div>
                <div className="flex justify-between items-center pt-4 border-t border-stone-100">
                  <div className="text-sm text-stone-600">
                    <p>{order.receiverName} {order.receiverPhone}</p>
                    <p className="text-xs text-stone-400 mt-1">{order.receiverAddress}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-sm text-stone-500">实付</p>
                    <p className="text-lg font-medium text-stone-900">¥{order.payAmount.toFixed(2)}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}
