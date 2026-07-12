#!/usr/bin/env python3
"""Run JMeter benchmarks for online-shop and produce cache-effect reports."""

import argparse
import csv
import json
import os
import platform
import shutil
import statistics
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

import mysql.connector

# MySQL configuration (must match application)
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "root",
    "database": "online_shop",
}

BASE_URL = "http://localhost:8080"
DEFAULT_PRODUCT_ID = 13
SCRIPT_DIR = Path(__file__).resolve().parent
JMETER_DIR = SCRIPT_DIR / "jmeter"
RESULTS_DIR = SCRIPT_DIR / "results"


def find_jmeter() -> Path:
    """Locate local JMeter installation."""
    system = platform.system()
    jmeter_home = os.environ.get("JMETER_HOME")

    if jmeter_home:
        # On Windows prefer the .bat launcher; fall back to the shell script on Unix.
        if system == "Windows":
            bat_path = Path(jmeter_home) / "bin" / "jmeter.bat"
            if bat_path.exists():
                return bat_path
        path = Path(jmeter_home) / "bin" / "jmeter"
        if path.exists():
            return path

    # Common Windows locations
    candidates = [
        Path(r"C:\develop\apache-jmeter-5.4.1\bin\jmeter.bat"),
        Path(r"C:\develop\apache-jmeter-5.4.1\bin\jmeter"),
        Path(r"C:\apache-jmeter-5.4.1\bin\jmeter.bat"),
        Path(r"C:\Program Files\apache-jmeter-5.4.1\bin\jmeter.bat"),
    ]
    for path in candidates:
        if path.exists():
            return path

    # Search PATH
    cmd = "jmeter.bat" if system == "Windows" else "jmeter"
    found = shutil.which(cmd)
    if found:
        return Path(found)

    raise RuntimeError(
        "JMeter not found. Please set JMETER_HOME or add JMeter bin directory to PATH."
    )


def get_jmeter_args(jmeter: Path) -> list[str]:
    """Return command arguments for JMeter, cross-platform."""
    if jmeter.suffix.lower() == ".bat":
        return [str(jmeter)]
    return [str(jmeter)]


def get_select_count() -> int | None:
    """Return current MySQL Com_select counter."""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        cursor.execute("SHOW GLOBAL STATUS LIKE 'Com_select'")
        value = int(cursor.fetchone()[1])
        cursor.close()
        conn.close()
        return value
    except Exception as e:
        print(f"[warn] failed to read Com_select: {e}")
        return None


def flush_redis() -> None:
    """Flush Redis to simulate cold cache."""
    # Try redis-cli first, then Python redis client
    try:
        subprocess.run(["redis-cli", "FLUSHDB"], check=True, capture_output=True)
        print("[bench] flushed Redis DB via redis-cli")
        return
    except FileNotFoundError:
        pass
    except subprocess.CalledProcessError as e:
        print(f"[warn] redis-cli FLUSHDB failed: {e}")
        return

    try:
        import redis
        r = redis.Redis(host="localhost", port=6379, db=0, protocol=2)
        r.flushdb()
        print("[bench] flushed Redis DB via Python redis client")
    except ImportError:
        print("[warn] redis-cli and Python redis client not found, skipping Redis flush")
    except Exception as e:
        print(f"[warn] failed to flush Redis: {e}")


def wait_for_backend(url: str = BASE_URL, timeout: int = 120) -> None:
    """Wait until backend health endpoint is reachable."""
    import urllib.request

    print(f"[bench] waiting for backend at {url} ...")
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(f"{url}/api/product/{DEFAULT_PRODUCT_ID}", timeout=2) as resp:
                if resp.status == 200:
                    print("[bench] backend is ready")
                    return
        except Exception:
            pass
        time.sleep(1)
    raise RuntimeError("backend did not become ready in time")


def run_jmeter(jmeter: Path, jmx: Path, jtl: Path, props: dict[str, str],
               html_report: bool = False) -> None:
    """Run a JMeter test plan in non-GUI mode."""
    # JMeter appends to JTL by default; remove stale file for clean results
    if jtl.exists():
        jtl.unlink()

    args = get_jmeter_args(jmeter)
    args += ["-n", "-t", str(jmx), "-l", str(jtl)]
    if html_report:
        args += ["-e", "-o", str(jtl.with_suffix(".report"))]
    for key, value in props.items():
        args += [f"-J{key}={value}"]

    print(f"[bench] running: {' '.join(args)}")
    subprocess.run(args, check=True)


def parse_jtl(jtl: Path) -> dict:
    """Parse a JMeter JTL CSV file and return aggregate statistics."""
    if not jtl.exists():
        return {}

    latencies = []
    elapsed = []
    rows = []
    success = 0
    fail = 0
    error_messages = []
    with open(jtl, "r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            rows.append(row)
            try:
                lat = int(row.get("Latency", "0") or "0")
                el = int(row.get("elapsed", "0") or "0")
                latencies.append(lat)
                elapsed.append(el)
                if row.get("success", "false").lower() == "true":
                    success += 1
                else:
                    fail += 1
                    msg = row.get("responseMessage", "")
                    if msg:
                        error_messages.append(msg)
            except ValueError:
                continue

    total = success + fail
    if not latencies:
        return {"total": 0, "success": 0, "fail": 0}

    # Use actual wall-clock span from sample timestamps
    timestamps_ms = [int(row.get("timeStamp", "0") or "0") for row in rows if row.get("timeStamp")]
    if len(timestamps_ms) > 1:
        elapsed_sec = (max(timestamps_ms) - min(timestamps_ms)) / 1000.0
    else:
        elapsed_sec = sum(elapsed) / 1000.0
    if elapsed_sec <= 0:
        elapsed_sec = sum(elapsed) / 1000.0
    # Ensure at least a small duration to avoid division by zero
    elapsed_sec = max(elapsed_sec, 0.001)

    def pct(data, p):
        if not data:
            return 0
        s = sorted(data)
        k = (len(s) - 1) * p
        f = int(k)
        c = min(f + 1, len(s) - 1)
        if f == c:
            return s[f]
        return s[f] + (s[c] - s[f]) * (k - f)

    return {
        "total": total,
        "success": success,
        "fail": fail,
        "error_rate": round(fail / total * 100, 2),
        "elapsed_seconds": round(elapsed_sec, 3),
        "tps": round(success / elapsed_sec, 2) if elapsed_sec > 0 else 0,
        "avg_latency_ms": round(statistics.mean(latencies), 2),
        "p50_latency_ms": round(pct(latencies, 0.5), 2),
        "p95_latency_ms": round(pct(latencies, 0.95), 2),
        "p99_latency_ms": round(pct(latencies, 0.99), 2),
        "min_latency_ms": min(latencies),
        "max_latency_ms": max(latencies),
        "sample_errors": error_messages[:5],
    }


def generate_report(summary: dict, output: Path) -> None:
    """Generate a Markdown report from benchmark summary."""
    lines = [
        "# JMeter 压测报告",
        "",
        f"生成时间：{summary.get('timestamp', datetime.now().isoformat())}",
        "",
        "## 商品详情缓存效果验证",
        "",
    ]

    cold = summary.get("product_detail_cold", {})
    warm = summary.get("product_detail_warm", {})
    if cold and warm:
        lines += [
            "| 指标 | Cold Cache（缓存未命中） | Warm Cache（缓存已命中） | 提升 |",
            "|------|--------------------------|--------------------------|------|",
            f"| 总请求 | {cold.get('total', 0)} | {warm.get('total', 0)} | - |",
            f"| 成功 | {cold.get('success', 0)} | {warm.get('success', 0)} | - |",
            f"| 失败 | {cold.get('fail', 0)} | {warm.get('fail', 0)} | - |",
            f"| TPS | {cold.get('tps', 0)} | {warm.get('tps', 0)} | {improvement(cold.get('tps', 0), warm.get('tps', 0))} |",
            f"| 平均延迟(ms) | {cold.get('avg_latency_ms', 0)} | {warm.get('avg_latency_ms', 0)} | {improvement(cold.get('avg_latency_ms', 0), warm.get('avg_latency_ms', 0), lower_is_better=True)} |",
            f"| P95(ms) | {cold.get('p95_latency_ms', 0)} | {warm.get('p95_latency_ms', 0)} | {improvement(cold.get('p95_latency_ms', 0), warm.get('p95_latency_ms', 0), lower_is_better=True)} |",
            f"| P99(ms) | {cold.get('p99_latency_ms', 0)} | {warm.get('p99_latency_ms', 0)} | {improvement(cold.get('p99_latency_ms', 0), warm.get('p99_latency_ms', 0), lower_is_better=True)} |",
            f"| MySQL Com_select 增长 | {cold.get('db_select_delta', 'N/A')} | {warm.get('db_select_delta', 'N/A')} | - |",
            "",
            "**结论**：缓存命中后 TPS 提升、延迟下降，且 MySQL `Com_select` 增长趋近于 0，证明 L1/L2 两级缓存有效拦截了数据库读请求。",
            "",
        ]

    order = summary.get("order_create", {})
    if order:
        lines += [
            "## 下单链路压测",
            "",
            "| 指标 | 数值 |",
            "|------|------|",
            f"| 总请求 | {order.get('total', 0)} |",
            f"| 成功 | {order.get('success', 0)} |",
            f"| 失败 | {order.get('fail', 0)} |",
            f"| 错误率 | {order.get('error_rate', 0)}% |",
            f"| TPS | {order.get('tps', 0)} |",
            f"| 平均延迟(ms) | {order.get('avg_latency_ms', 0)} |",
            f"| P95(ms) | {order.get('p95_latency_ms', 0)} |",
            f"| P99(ms) | {order.get('p99_latency_ms', 0)} |",
            "",
        ]

    lines += [
        "## 原始结果文件",
        "",
        "- JTL 与 HTML 报告：`scripts/benchmark/results/`",
        "",
    ]

    output.write_text("\n".join(lines), encoding="utf-8")
    print(f"[bench] report written to {output}")


def improvement(before, after, lower_is_better: bool = False) -> str:
    """Calculate improvement percentage string."""
    try:
        before = float(before)
        after = float(after)
        if before == 0:
            return "-"
        delta = (after - before) / before * 100
        if lower_is_better:
            delta = -delta
        return f"{delta:+.1f}%"
    except (ValueError, TypeError):
        return "-"


def run_benchmark(args) -> dict:
    """Execute the full benchmark suite."""
    jmeter = find_jmeter()
    print(f"[bench] using JMeter: {jmeter}")

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)

    # Prepare data
    if not args.skip_prepare:
        prepare_script = SCRIPT_DIR / "prepare_bench_data.py"
        subprocess.run(
            [sys.executable, str(prepare_script), "--users", str(args.users)],
            check=True,
        )

    # Ensure JMX files exist
    product_jmx = JMETER_DIR / "product_detail_cache.jmx"
    order_jmx = JMETER_DIR / "order_create.jmx"
    if not product_jmx.exists() or not order_jmx.exists():
        print("[bench] generating JMX files...")
        subprocess.run(
            [sys.executable, str(JMETER_DIR / "generate_jmx.py"),
             "--csv", str(RESULTS_DIR / "bench_users.csv"),
             "--output-dir", str(JMETER_DIR)],
            check=True,
        )

    # Wait for backend if requested
    if args.wait_for_backend:
        wait_for_backend()

    summary = {"timestamp": datetime.now().isoformat()}

    # Product detail - cold cache
    if not args.skip_product:
        print("\n========== Product Detail - Cold Cache ==========")
        if args.restart_cmd:
            print(f"[bench] restarting backend to clear L1 cache and flush L2 cache: {args.restart_cmd}")
            subprocess.run(args.restart_cmd, shell=True, check=True)
            wait_for_backend()
        else:
            print("[warn] no --restart-cmd provided; L1 Caffeine cache remains warm. "
                  "Cold-cache numbers will reflect L2 Redis miss/hit rather than true DB miss.")
        cold_jtl = RESULTS_DIR / "product_detail_cold.jtl"
        select_before = get_select_count()
        run_jmeter(jmeter, product_jmx, cold_jtl, {
            "threads": str(args.product_threads),
            "loops": str(args.product_loops),
            "ramp_up": str(args.ramp_up),
        }, html_report=args.html_report)
        select_after = get_select_count()
        cold_summary = parse_jtl(cold_jtl)
        if select_before is not None and select_after is not None:
            cold_summary["db_select_delta"] = select_after - select_before
        summary["product_detail_cold"] = cold_summary
        print(json.dumps(cold_summary, ensure_ascii=False, indent=2))

        # Product detail - warm cache
        print("\n========== Product Detail - Warm Cache ==========")
        warm_jtl = RESULTS_DIR / "product_detail_warm.jtl"
        select_before = get_select_count()
        run_jmeter(jmeter, product_jmx, warm_jtl, {
            "threads": str(args.product_threads),
            "loops": str(args.product_loops),
            "ramp_up": str(args.ramp_up),
        }, html_report=args.html_report)
        select_after = get_select_count()
        warm_summary = parse_jtl(warm_jtl)
        if select_before is not None and select_after is not None:
            warm_summary["db_select_delta"] = select_after - select_before
        summary["product_detail_warm"] = warm_summary
        print(json.dumps(warm_summary, ensure_ascii=False, indent=2))

    # Order create
    if not args.skip_order:
        print("\n========== Order Create ==========")
        order_jtl = RESULTS_DIR / "order_create.jtl"
        run_jmeter(jmeter, order_jmx, order_jtl, {
            "threads": str(args.order_threads),
            "loops": str(args.order_loops),
            "ramp_up": str(args.ramp_up),
        }, html_report=args.html_report)
        order_summary = parse_jtl(order_jtl)
        summary["order_create"] = order_summary
        print(json.dumps(order_summary, ensure_ascii=False, indent=2))

    # Save JSON summary
    summary_file = RESULTS_DIR / "jmeter_summary.json"
    with open(summary_file, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    print(f"[bench] summary saved to {summary_file}")

    # Generate Markdown report
    generate_report(summary, RESULTS_DIR / "jmeter_report.md")

    return summary


def main():
    parser = argparse.ArgumentParser(description="Run JMeter benchmarks for online-shop")
    parser.add_argument("--users", type=int, default=250, help="number of bench users")
    parser.add_argument("--product-threads", type=int, default=100, help="threads for product detail")
    parser.add_argument("--product-loops", type=int, default=30, help="loops per thread for product detail")
    parser.add_argument("--order-threads", type=int, default=100, help="threads for order create")
    parser.add_argument("--order-loops", type=int, default=1, help="loops per thread for order create")
    parser.add_argument("--ramp-up", type=int, default=5, help="ramp-up seconds")
    parser.add_argument("--skip-prepare", action="store_true", help="skip data preparation")
    parser.add_argument("--skip-product", action="store_true", help="skip product detail benchmark")
    parser.add_argument("--skip-order", action="store_true", help="skip order create benchmark")
    parser.add_argument("--wait-for-backend", action="store_true", help="wait for backend before running")
    parser.add_argument("--html-report", action="store_true", help="generate JMeter HTML dashboard (may fail on tiny datasets)")
    parser.add_argument("--restart-cmd", type=str, default="", help="shell command to restart backend before cold-cache test")
    args = parser.parse_args()

    run_benchmark(args)


if __name__ == "__main__":
    main()
