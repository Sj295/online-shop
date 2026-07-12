# Benchmark Scripts

本目录包含 online-shop 的压测脚本，支持 Python 原生脚本与 Apache JMeter 两种模式。

## 环境要求

- Python 3.10+
- MySQL 8（本地默认 `root` / `root`）
- Redis 7（本地默认端口 6379）
- Apache JMeter 5.4+（使用 JMeter 模式时）

## 安装依赖

```bash
pip install -r requirements.txt
```

## 准备压测数据

```bash
python prepare_bench_data.py --users 250
```

该脚本会：

1. 执行 `sql/init.sql` 重置数据库
2. 创建 `bench_0001` ~ `bench_0250` 共 250 个压测用户、地址、购物车
3. 生成 `results/bench_users.json`（Python 脚本使用）
4. 生成 `results/bench_users.csv`（JMeter 使用）

## JMeter 压测（推荐）

### 生成测试计划

```bash
python jmeter/generate_jmx.py --csv results/bench_users.csv
```

会生成：

- `jmeter/product_detail_cache.jmx`：商品详情 L1/L2 缓存效果验证
- `jmeter/order_create.jmx`：下单链路压测

### 执行压测

```bash
# 基础执行（假设后端已在 localhost:8080 运行）
python run_jmeter_benchmark.py

# 等待后端启动后再执行
python run_jmeter_benchmark.py --wait-for-backend

# 带应用重启，完整展示 L1/L2 缓存冷热差异（会中断本地服务）
python run_jmeter_benchmark.py --wait-for-backend \
  --restart-cmd "pkill -f 'shop-portal'; cd ../../backend/shop-portal && nohup mvn spring-boot:run -DskipTests > portal.log 2>&1 &"

# 生成 JMeter HTML Dashboard
python run_jmeter_benchmark.py --html-report
```

### 输出文件

- `results/jmeter_summary.json`：JSON 格式摘要
- `results/jmeter_report.md`：Markdown 报告
- `results/product_detail_cold.jtl`：冷缓存原始采样
- `results/product_detail_warm.jtl`：热缓存原始采样
- `results/order_create.jtl`：下单链路原始采样
- `results/*.report/index.html`：JMeter HTML Dashboard（加 `--html-report` 时生成）

## Python 原生压测

仍保留原脚本，适用于快速验证：

```bash
# 商品详情缓存验证
python benchmark_product_detail.py --total 3000 --workers 100 --warmup 0 --output results/product_detail_cold.json
python benchmark_product_detail.py --total 3000 --workers 100 --warmup 100 --output results/product_detail_warm.json

# 下单链路
python benchmark_order_create.py --users 250 --requests-per-user 1 --output results/order_create.json
```

## GitHub Actions

仓库已配置 `.github/workflows/benchmark.yml`，会在修改后端或压测脚本时自动执行 JMeter 压测，并在 Actions 运行摘要中展示结果表格。
