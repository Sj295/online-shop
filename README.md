# BOUTIQUE 在线商城

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen)
![React](https://img.shields.io/badge/React-19-61DAFB)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4)
![License](https://img.shields.io/badge/License-MIT-yellow)

一个以中性色、极简网格与高级感字体为设计基调的在线商城系统。后端采用 Java + Spring Boot 3 + MySQL + Redis 自实现，前端使用 React + Tailwind CSS + Framer Motion。

<p align="center">
  <img src="frontend/public/screenshots/home.png" alt="首页" width="800">
</p>

## 技术栈

### 后端
- Java 17 + Maven
- Spring Boot 3.2
- MyBatis-Plus 3.5.6
- Sa-Token 1.38（认证鉴权）
- Redisson 3.27（分布式锁）
- Redis 7（缓存、会话）
- MySQL 8
- Knife4j（OpenAPI 文档）

### 前端
- React 19 + TypeScript
- Vite 6
- Tailwind CSS 4
- Zustand（状态管理）
- Framer Motion（微动效）
- React Router 7
- Axios

## 项目结构

```
online-shop/
├── backend/
│   ├── shop-common/       # 公共模块：实体、Mapper、Service、工具、配置
│   ├── shop-portal/       # 前台用户端接口（端口 8080）
│   ├── shop-admin/        # 管理后台接口（端口 8081）
│   └── pom.xml
├── frontend/              # React 前端（端口 5173）
│   └── public/screenshots/
├── sql/
│   ├── schema.sql         # 数据库结构
│   ├── data.sql           # 演示数据
│   ├── init.sql           # 一键初始化（DROP + schema + data）
│   └── optimize_indexes.sql # 生产环境索引/版本号/归档表优化脚本
├── scripts/benchmark/     # 压测脚本与结果
│   ├── prepare_bench_data.py
│   ├── benchmark_order_create.py
│   └── results/
└── README.md
```

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.x，账号 `root` / `root`
- Redis 7.x（本地默认端口 6379）

## 快速启动

### 1. 初始化数据库

```bash
mysql -uroot -proot --default-character-set=utf8mb4 < sql/init.sql

# 生产环境追加索引/乐观锁/归档表（可选）
mysql -uroot -proot --default-character-set=utf8mb4 < sql/optimize_indexes.sql
```

### 2. 启动后端

```bash
cd backend
mvn clean install -DskipTests

# 需要两个终端分别启动
cd shop-portal && mvn spring-boot:run -DskipTests
cd shop-admin && mvn spring-boot:run -DskipTests
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

浏览器访问：http://localhost:5173

## 演示账号

| 角色 | 用户名 | 密码 |
|---|---|---|
| 普通用户 | user | 123456 |
| 管理员 | admin | 123456 |

## 接口文档

- 前台接口文档：http://localhost:8080/doc.html
- 管理后台接口文档：http://localhost:8081/doc.html

## 界面预览

| 首页 | 商品详情 | 订单中心 |
|---|---|---|
| ![首页](frontend/public/screenshots/home.png) | ![商品详情](frontend/public/screenshots/product-detail.png) | ![订单中心](frontend/public/screenshots/orders.png) |

## 已实现功能

### 前台
- 首页：轮播图、商品分类、热销推荐、新品上市
- 商品列表与分类筛选
- 商品搜索
- 商品详情
- 购物车（增删改、选中、数量调整）
- 订单创建与模拟支付
- 订单列表与状态筛选
- 用户登录/注册

### 管理后台
- 管理员登录
- 商品管理（增删改查、上下架）
- 分类管理（增删改查）
- 订单管理（列表、发货）
- 用户管理（列表、状态调整）

## 高并发四层防御架构

针对 `POST /api/order/create` 下单链路，按“流量逐层递减”的思路做了四层递进式优化：

```
客户端请求
    │
    ▼
┌─────────────────────────────────────┐
│ Layer 1：应用层                      │  L1 Caffeine + L2 Redis 两级缓存
│  · 接口限流（Redis + Lua 滑动窗口）  │  Resilience4j 熔断 / 降级
│  · Sa-Token 无状态登录              │
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Layer 2：缓存层                      │  布隆过滤器防穿透
│  · 热点 key 分布式锁重建缓存         │  TTL 抖动防止缓存雪崩
│  · Redis Lua 原子库存预扣            │  DB CAS 最终兜底
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Layer 3：异步队列层                  │  Redis List 异步任务队列
│  · 下单先预扣库存再入队，立即返回    │  后台恒定速率 Worker 消费落库
│  · UserID+商品维度幂等锁             │  GET /api/order/create/status 轮询
└─────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────┐
│ Layer 4：存储层                      │  订单明细批量插入（INSERT ... VALUES）
│  · HikariCP + Redis Lettuce 连接池调优│  MyBatis-Plus @Version 乐观锁
│  · 关键索引与订单归档表              │  库存 CAS 兜底
└─────────────────────────────────────┘
```

### 关键代码位置

| 层级 | 能力 | 主要文件 |
|------|------|----------|
| Layer 1 | L1/L2 缓存 | `backend/shop-common/src/main/java/com/shop/common/cache/TwoLevelCache.java` |
| Layer 1 | 限流 | `backend/shop-common/src/main/java/com/shop/common/limit/RateLimiter.java`、 `scripts/rate_limit.lua` |
| Layer 1 | 熔断 | `backend/shop-common/src/main/java/com/shop/common/config/Resilience4jConfig.java` |
| Layer 2 | 布隆过滤器 + 缓存重建 | `backend/shop-common/src/main/java/com/shop/common/bloom/BloomFilterService.java`、 `ProductServiceImpl.java` |
| Layer 2 | Lua 原子库存 | `backend/shop-common/src/main/java/com/shop/common/stock/RedisStockService.java`、 `scripts/stock_deduct.lua` |
| Layer 3 | 异步队列 | `backend/shop-common/src/main/java/com/shop/common/mq/OrderCreateProducer.java`、 `OrderCreateConsumer.java` |
| Layer 3 | 超时兜底 | `backend/shop-common/src/main/java/com/shop/common/mq/OrderTimeoutRefundJob.java` |
| Layer 3 | 前端轮询 | `frontend/src/pages/CartPage.tsx` |
| Layer 4 | 批量插入 | `backend/shop-common/src/main/resources/mapper/OrderItemMapper.xml` |
| Layer 4 | 连接池调优 | `backend/shop-portal/src/main/resources/application.yml` |
| Layer 4 | 归档 | `backend/shop-common/src/main/java/com/shop/common/archive/OrderArchiveService.java` |

### 压测结果

环境：本地单实例 MySQL 8 + Redis 3.2.100，250 个独立用户各下单 1 次（商品 13 初始库存 300）。
Layer 3/4 为异步流程，压测脚本会轮询 `/api/order/create/status` 直到 SUCCESS/FAILED。

| 指标 | Baseline | Layer 1 | Layer 2 | Layer 3 | Layer 4 |
|------|----------|---------|---------|---------|---------|
| 总请求 | 250 | 250 | 250 | 250 | 250 |
| 成功下单 | 249 | 189 | 189 | 187 | 189 |
| 业务失败 | 1 | 61 | 61 | 63 | 61 |
| 超时 | 0 | 0 | 0 | 0 | 0 |
| 错误 | 0 | 0 | 0 | 0 | 0 |
| 耗时(s) | 1.665 | 2.157 | 3.113 | 2.418 | 2.764 |
| TPS | 149.58 | 87.62 | 60.71 | 77.34 | 68.39 |
| 平均延迟(ms) | 289.72 | 377.95 | 543.31 | 57.23 | 42.21 |
| P50(ms) | 310.37 | 462.14 | 728.77 | 32.67 | 32.47 |
| P95(ms) | 331.28 | 634.62 | 899.64 | 194.08 | 105.37 |
| P99(ms) | 339.80 | 915.75 | 1352.06 | 228.96 | 146.93 |
| 初始库存 | 300 | 300 | 300 | 300 | 300 |
| 已售 | 250 | 190 | 190 | 189 | 190 |
| 当前库存 | 50 | 110 | 110 | 111 | 110 |
| 超卖数 | 0 | 0 | 0 | 0 | 0 |

说明：
- **Baseline**：同步落库，TPS 最高，但所有压力直接打到 MySQL，P99 接近 340ms。
- **Layer 1/2**：增加缓存与 Redis 预扣后，同步链路变长，TPS 下降、P99 上升；但请求量被缓存层拦截，MySQL 压力显著降低。
- **Layer 3/4**：改为异步队列后，接口立即返回 `PENDING`，平均/P95/P99 延迟大幅下降；Worker 恒定速率消费，流量被削峰填谷，数据库压力进一步降低。
- 所有层级均保持 **超卖数为 0**，库存一致性通过“Redis Lua 预扣 + DB CAS 兜底 + 失败回滚”保证。

### 压测脚本使用

```bash
cd scripts/benchmark

# 重置数据库并创建压测用户（每次压测前执行）
python prepare_bench_data.py --users 250

# Baseline / Layer 1 / Layer 2（同步下单）
python benchmark_order_create.py --users 250 --requests-per-user 1 --output results/baseline.json

# Layer 3 / Layer 4（异步下单，需要轮询状态）
python benchmark_order_create.py --users 250 --requests-per-user 1 --poll --output results/layer3.json
```

原始数据文件位于 `scripts/benchmark/results/`。

## 商品图片视觉规范

- 统一采用 4:5 竖版比例容器
- 图片使用 `object-cover` 填充，保持视觉一致性
- 圆角 `rounded-xl` / `rounded-2xl`，柔和过渡
- 中性灰背景占位，避免加载时突兀
- 卡片无粗边框，使用阴影与留白体现层级
