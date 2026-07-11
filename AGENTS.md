# AGENTS.md — BOUTIQUE 在线商城

本项目为前后端分离的在线商城系统，采用 React 前端 + Java/Spring Boot 后端 + MySQL + Redis。

## 项目结构

```
online-shop/
├── backend/
│   ├── shop-common/          # 公共模块：实体、Mapper、Service、工具、配置
│   ├── shop-portal/          # 前台用户端接口（端口 8080）
│   ├── shop-admin/           # 管理后台接口（端口 8081）
│   └── pom.xml
├── frontend/                 # React + Vite + Tailwind CSS（端口 5173）
├── sql/
│   ├── schema.sql            # 数据库结构
│   ├── data.sql              # 演示数据
│   └── init.sql              # 一键初始化（DROP + schema + data）
├── README.md
└── AGENTS.md                 # 本文件
```

## 技术栈

- **后端**：Java 17、Spring Boot 3.2、MyBatis-Plus 3.5.6、Sa-Token 1.38、Redisson 3.27、MySQL 8、Redis 7、Knife4j
- **前端**：React 19、TypeScript、Vite 6、Tailwind CSS 4、Zustand、Framer Motion、React Router 7、Axios

## 环境依赖

- JDK 17+
- Node.js 18+
- MySQL 8.x（账号 `root` / `root`）
- Redis 7.x（本地默认端口 6379）

## 开发启动流程

### 1. 初始化数据库

```bash
mysql -uroot -proot --default-character-set=utf8mb4 < sql/init.sql
```

### 2. 启动后端

在项目根目录执行：

```bash
cd backend
mvn clean install -DskipTests
```

分别启动前台和管理后台（需要两个终端）：

```bash
cd backend/shop-portal
mvn spring-boot:run -DskipTests
```

```bash
cd backend/shop-admin
mvn spring-boot:run -DskipTests
```

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

## 构建验证

- 后端：`cd backend && mvn clean install -DskipTests`
- 前端：`cd frontend && npm run build`

## 关键约定

- 后端统一返回格式：`{ code, message, data }`，`code === 200` 为成功
- 前端 API 封装在 `frontend/src/api/`，request 拦截器自动从 `localStorage` 读取 `token` 并写入 `Authorization` header
- 前端状态管理：`frontend/src/store/auth.ts` 和 `frontend/src/store/cart.ts`
- 前端风格：中性色（stone/slate）、极简网格、4:5 商品图比例、充足留白、Framer Motion 微动效
- 高可用：Redisson 分布式锁扣减库存、Redis 缓存、接口幂等、全局异常处理

## 演示账号

- 普通用户：`user` / `123456`
- 管理员：`admin` / `123456`

## 修改代码前的注意事项

1. 后端修改后需要重新 `mvn clean install -DskipTests`，并重启对应服务
2. 前端修改由 Vite 热更新自动生效；如修改了依赖或配置，需重新 `npm install` 并重启
3. 数据库演示数据可通过 `sql/init.sql` 重新初始化
4. 前端 API 默认代理到 `http://localhost:8080`，管理后台 API 直接请求 `http://localhost:8081`

## 常见问题

- 若前端无法访问后端，请确认 `shop-portal` 已启动且 Redis/MySQL 可连接
- 若登录提示失败，可能是 `data.sql` 中密码 hash 与实际不匹配，可重新执行 `sql/init.sql`
- 管理后台登录失败时，检查 `shop-admin` 是否已启动，以及是否访问了正确的 `/api/admin/login` 接口
