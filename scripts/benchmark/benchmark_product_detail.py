#!/usr/bin/env python3
"""Benchmark GET /api/product/{id} to verify L1/L2 cache effect."""

import argparse
import json
import os
import statistics
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

import mysql.connector
import requests

BASE_URL = "http://localhost:8080"
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "root",
    "database": "online_shop",
}


def get_select_count():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()
        cursor.execute("SHOW GLOBAL STATUS LIKE 'Com_select'")
        value = int(cursor.fetchone()[1])
        cursor.close()
        conn.close()
        return value
    except Exception:
        return None


def fetch(product_id: int):
    url = f"{BASE_URL}/api/product/{product_id}"
    start = time.perf_counter()
    try:
        resp = requests.get(url, timeout=10)
        latency_ms = (time.perf_counter() - start) * 1000
        return {
            "status": "success" if resp.status_code == 200 and resp.json().get("code") == 200 else "fail",
            "latency_ms": latency_ms,
        }
    except requests.Timeout:
        return {"status": "timeout", "latency_ms": (time.perf_counter() - start) * 1000}
    except Exception as e:
        return {"status": "error", "message": str(e), "latency_ms": (time.perf_counter() - start) * 1000}


def run_benchmark(total: int, workers: int, product_id: int, warmup: int = 0):
    if warmup > 0:
        print(f"[bench] warming cache with {warmup} sequential requests...")
        for _ in range(warmup):
            fetch(product_id)

    print(f"[bench] GET /api/product/{product_id}: {total} requests, {workers} workers")
    select_before = get_select_count()
    results = []
    start_total = time.perf_counter()
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = [ex.submit(fetch, product_id) for _ in range(total)]
        for future in as_completed(futures):
            results.append(future.result())
    elapsed = time.perf_counter() - start_total
    select_after = get_select_count()
    summary = analyze(results, elapsed)
    if select_before is not None and select_after is not None:
        summary["db_select_delta"] = select_after - select_before
    return summary


def analyze(results, elapsed):
    total = len(results)
    success = [r for r in results if r["status"] == "success"]
    latencies = [r["latency_ms"] for r in results]

    summary = {
        "total": total,
        "success": len(success),
        "fail": total - len(success),
        "elapsed_seconds": round(elapsed, 3),
        "tps": round(len(success) / elapsed, 2) if elapsed > 0 else 0,
        "avg_latency_ms": round(statistics.mean(latencies), 2) if latencies else 0,
        "p50_latency_ms": round(percentile(latencies, 0.5), 2) if latencies else 0,
        "p95_latency_ms": round(percentile(latencies, 0.95), 2) if latencies else 0,
        "p99_latency_ms": round(percentile(latencies, 0.99), 2) if latencies else 0,
        "db_select_delta": 0,
        "timestamp": datetime.now().isoformat(),
    }
    return summary


def percentile(data, p):
    if not data:
        return 0
    s = sorted(data)
    k = (len(s) - 1) * p
    f = int(k)
    c = min(f + 1, len(s) - 1)
    if f == c:
        return s[f]
    return s[f] + (s[c] - s[f]) * (k - f)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--product-id", type=int, default=13)
    parser.add_argument("--total", type=int, default=1000)
    parser.add_argument("--workers", type=int, default=100)
    parser.add_argument("--warmup", type=int, default=0, help="number of sequential warmup requests before benchmark")
    parser.add_argument("--output", type=str, default="product_detail.json")
    args = parser.parse_args()

    summary = run_benchmark(args.total, args.workers, args.product_id, args.warmup)

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    print("\n========== Product Detail Benchmark Result ==========")
    print(f"total: {summary['total']}")
    print(f"success: {summary['success']}")
    print(f"fail: {summary['fail']}")
    print(f"elapsed: {summary['elapsed_seconds']}s")
    print(f"tps: {summary['tps']}")
    print(f"avg_latency: {summary['avg_latency_ms']}ms")
    print(f"p50_latency: {summary['p50_latency_ms']}ms")
    print(f"p95_latency: {summary['p95_latency_ms']}ms")
    print(f"p99_latency: {summary['p99_latency_ms']}ms")
    print(f"db_select_delta: {summary.get('db_select_delta', 'N/A')}")
    print(f"saved to {args.output}")


if __name__ == "__main__":
    main()
