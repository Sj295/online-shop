#!/usr/bin/env python3
"""Benchmark POST /api/order/create across all optimization layers."""

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

DEFAULT_PRODUCT_ID = 13


def login(username: str, password: str = "123456"):
    url = f"{BASE_URL}/api/user/login"
    try:
        resp = requests.post(url, json={"username": username, "password": password}, timeout=10)
        data = resp.json()
        if data.get("code") == 200:
            return data["data"]
        print(f"[login failed] {username}: {data}")
    except Exception as e:
        print(f"[login error] {username}: {e}")
    return None


BENCH_USERS_FILE = os.path.join(os.path.dirname(__file__), "results", "bench_users.json")


def load_bench_users(users: int):
    with open(BENCH_USERS_FILE, "r", encoding="utf-8") as f:
        mapping = json.load(f)
    usernames = [f"bench_{i + 1:04d}" for i in range(users)]
    return {u: mapping[u] for u in usernames if u in mapping}


def login_all(users: int, workers: int):
    user_map = load_bench_users(users)
    tokens = {}
    print(f"[bench] logging in {users} users with {workers} workers...")
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(login, u): u for u in user_map}
        for future in as_completed(futures):
            u = futures[future]
            token = future.result()
            if token:
                tokens[u] = {"token": token, "address_id": user_map[u]["address_id"]}
    print(f"[bench] {len(tokens)}/{users} users logged in")
    if not tokens:
        sys.exit(1)
    return tokens


def create_order(token: str, address_id: int):
    url = f"{BASE_URL}/api/order/create"
    headers = {"Authorization": token}
    payload = {"addressId": address_id, "remark": "benchmark"}
    start = time.perf_counter()
    try:
        resp = requests.post(url, headers=headers, json=payload, timeout=30)
        latency_ms = (time.perf_counter() - start) * 1000
        data = resp.json()
        code = data.get("code")
        order_no = None
        raw_data = data.get("data")
        if isinstance(raw_data, dict):
            order_no = raw_data.get("orderNo")
        elif isinstance(raw_data, str):
            order_no = raw_data
        return {
            "status": "success" if code == 200 and order_no else "fail",
            "code": code,
            "message": data.get("message"),
            "order_no": order_no,
            "latency_ms": latency_ms,
        }
    except requests.Timeout:
        return {"status": "timeout", "latency_ms": (time.perf_counter() - start) * 1000}
    except Exception as e:
        return {"status": "error", "message": str(e), "latency_ms": (time.perf_counter() - start) * 1000}


def query_status(token: str, order_no: str, max_attempts: int = 60, interval: float = 0.5):
    url = f"{BASE_URL}/api/order/create/status"
    headers = {"Authorization": token}
    for attempt in range(max_attempts):
        try:
            resp = requests.get(url, headers=headers, params={"orderNo": order_no}, timeout=10)
            data = resp.json()
            if data.get("code") == 200:
                status = data.get("data", {}).get("status")
                if status == "SUCCESS":
                    return "success"
                if status == "FAILED":
                    return "fail"
        except Exception:
            pass
        time.sleep(interval)
    return "timeout"


def run_create(token_info: dict, poll: bool):
    result = create_order(token_info["token"], token_info["address_id"])
    if result["status"] == "success" and result.get("order_no") and poll:
        final = query_status(token_info["token"], result["order_no"])
        result["final_status"] = final
        if final != "success":
            result["status"] = final
    return result


def run_benchmark(users: int, requests_per_user: int, workers: int, poll: bool):
    tokens = login_all(users, workers)
    token_list = list(tokens.values())

    # warmup
    print("[bench] warmup 10 requests...")
    for _ in range(10):
        create_order(token_list[0]["token"], token_list[0]["address_id"])

    print(f"[bench] starting load test: {users} users * {requests_per_user} requests, workers={workers}")
    results = []
    start_total = time.perf_counter()

    tasks = []
    for i in range(users):
        for _ in range(requests_per_user):
            tasks.append(token_list[i % len(token_list)])

    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(run_create, token, poll): token for token in tasks}
        for future in as_completed(futures):
            results.append(future.result())

    elapsed = time.perf_counter() - start_total
    return analyze(results, elapsed)


def analyze(results, elapsed):
    total = len(results)
    success = [r for r in results if r["status"] == "success"]
    business_fail = [r for r in results if r["status"] == "fail"]
    timeout = [r for r in results if r["status"] == "timeout"]
    error = [r for r in results if r["status"] == "error"]
    latencies = [r["latency_ms"] for r in results]

    summary = {
        "total": total,
        "success": len(success),
        "business_fail": len(business_fail),
        "timeout": len(timeout),
        "error": len(error),
        "elapsed_seconds": round(elapsed, 3),
        "tps": round(len(success) / elapsed, 2) if elapsed > 0 else 0,
        "avg_latency_ms": round(statistics.mean(latencies), 2) if latencies else 0,
        "p50_latency_ms": round(percentile(latencies, 0.5), 2) if latencies else 0,
        "p95_latency_ms": round(percentile(latencies, 0.95), 2) if latencies else 0,
        "p99_latency_ms": round(percentile(latencies, 0.99), 2) if latencies else 0,
        "oversold": 0,
        "timestamp": datetime.now().isoformat(),
    }
    return summary, results


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


def check_oversold(product_id: int):
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT stock FROM pms_product WHERE id = %s", (product_id,))
    current_stock = int(cursor.fetchone()[0])
    cursor.execute("SELECT IFNULL(SUM(quantity), 0) FROM oms_order_item WHERE product_id = %s", (product_id,))
    sold = int(cursor.fetchone()[0])
    cursor.close()
    conn.close()
    initial_stock = 300
    oversold = max(0, sold - initial_stock)
    return {"initial_stock": initial_stock, "current_stock": current_stock, "sold": sold, "oversold": oversold}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--users", type=int, default=250)
    parser.add_argument("--requests-per-user", type=int, default=1)
    parser.add_argument("--workers", type=int, default=50)
    parser.add_argument("--poll", action="store_true", help="poll status after create (required for async layer)")
    parser.add_argument("--product-id", type=int, default=DEFAULT_PRODUCT_ID)
    parser.add_argument("--output", type=str, default="benchmark.json")
    args = parser.parse_args()

    summary, _ = run_benchmark(args.users, args.requests_per_user, args.workers, args.poll)
    stock_info = check_oversold(args.product_id)
    summary.update(stock_info)

    os.makedirs(os.path.dirname(args.output) or ".", exist_ok=True)
    with open(args.output, "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    print("\n========== Benchmark Result ==========")
    print(f"total: {summary['total']}")
    print(f"success: {summary['success']}")
    print(f"business_fail: {summary['business_fail']}")
    print(f"timeout: {summary['timeout']}")
    print(f"error: {summary['error']}")
    print(f"elapsed: {summary['elapsed_seconds']}s")
    print(f"tps: {summary['tps']}")
    print(f"avg_latency: {summary['avg_latency_ms']}ms")
    print(f"p50_latency: {summary['p50_latency_ms']}ms")
    print(f"p95_latency: {summary['p95_latency_ms']}ms")
    print(f"p99_latency: {summary['p99_latency_ms']}ms")
    print(f"sold: {summary['sold']}, initial_stock: {summary['initial_stock']}, oversold: {summary['oversold']}")
    print(f"saved to {args.output}")


if __name__ == "__main__":
    main()
