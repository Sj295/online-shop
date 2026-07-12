#!/usr/bin/env python3
"""Prepare benchmark data: reset DB and create N users/addresses/carts."""

import argparse
import csv
import json
import os
import subprocess
import sys

import mysql.connector

DB_HOST = "localhost"
DB_PORT = 3306
DB_USER = "root"
DB_PASSWORD = "root"
DB_NAME = "online_shop"

PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
INIT_SQL = os.path.join(PROJECT_ROOT, "sql", "init.sql")

DEFAULT_PRODUCT_ID = 13
DEFAULT_SKU_ID = 13
DEFAULT_QUANTITY = 1
BCRYPT_PASSWORD = "$2a$10$9nAySRZ9GbPmzt./3dgVMekHb7CHpZCi1SSXwgJb2weDjIF6Luyw6"


def reset_database():
    print(f"[prepare] reset database via {INIT_SQL}")
    cmd = [
        "mysql",
        f"-h{DB_HOST}",
        f"-P{DB_PORT}",
        f"-u{DB_USER}",
        f"-p{DB_PASSWORD}",
        "--default-character-set=utf8mb4",
    ]
    try:
        with open(INIT_SQL, "r", encoding="utf-8") as f:
            subprocess.run(cmd, stdin=f, check=True, cwd=PROJECT_ROOT)
    except FileNotFoundError:
        print("[error] mysql command not found")
        sys.exit(1)
    except subprocess.CalledProcessError as e:
        print(f"[error] init.sql failed: {e}")
        sys.exit(1)


def create_bench_data(users: int, product_id: int, sku_id: int, quantity: int):
    conn = mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
    )
    cursor = conn.cursor()

    cursor.execute("SELECT stock FROM pms_product WHERE id = %s", (product_id,))
    row = cursor.fetchone()
    if not row:
        print(f"[error] product {product_id} not found")
        sys.exit(1)
    stock = row[0]
    if stock < users * quantity:
        print(f"[warn] stock {stock} < demand {users * quantity}")

    user_values = []
    address_values = []
    cart_values = []

    start_user_id = 3
    for i in range(users):
        user_id = start_user_id + i
        username = f"bench_{i + 1:04d}"
        user_values.append((user_id, username, BCRYPT_PASSWORD, f"bench_{i + 1}", f"138{i:08d}"))
        address_values.append((user_id, "Tester", f"138{i:08d}", "Beijing", "Beijing", "Chaoyang", f"Bench {i + 1}", 1))
        cart_values.append((user_id, product_id, sku_id, quantity, 1))

    cursor.executemany(
        "INSERT INTO ums_user (id, username, password, nickname, phone) VALUES (%s, %s, %s, %s, %s)",
        user_values,
    )
    cursor.executemany(
        "INSERT INTO ums_address (user_id, receiver_name, phone, province, city, district, detail, is_default) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
        address_values,
    )
    cursor.executemany(
        "INSERT INTO oms_cart_item (user_id, product_id, sku_id, quantity, selected) VALUES (%s, %s, %s, %s, %s)",
        cart_values,
    )

    conn.commit()

    cursor.execute(
        "SELECT u.username, u.id, a.id FROM ums_user u JOIN ums_address a ON u.id = a.user_id WHERE u.username LIKE 'bench_%' ORDER BY u.id"
    )
    user_map = {}
    for username, user_id, address_id in cursor:
        user_map[username] = {"user_id": int(user_id), "address_id": int(address_id)}

    results_dir = os.path.join(os.path.dirname(__file__), "results")
    os.makedirs(results_dir, exist_ok=True)
    with open(os.path.join(results_dir, "bench_users.json"), "w", encoding="utf-8") as f:
        json.dump(user_map, f, ensure_ascii=False, indent=2)

    # Generate CSV for JMeter
    csv_path = os.path.join(results_dir, "bench_users.csv")
    with open(csv_path, "w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["username", "addressId"])
        for username in sorted(user_map):
            writer.writerow([username, user_map[username]["address_id"]])

    cursor.close()
    conn.close()
    print(f"[prepare] created {users} bench users for product={product_id} sku={sku_id}")
    print(f"[prepare] JMeter CSV written to {csv_path}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--users", type=int, default=250)
    parser.add_argument("--product-id", type=int, default=DEFAULT_PRODUCT_ID)
    parser.add_argument("--sku-id", type=int, default=DEFAULT_SKU_ID)
    parser.add_argument("--quantity", type=int, default=DEFAULT_QUANTITY)
    parser.add_argument("--skip-reset", action="store_true")
    args = parser.parse_args()

    if not args.skip_reset:
        reset_database()
    create_bench_data(args.users, args.product_id, args.sku_id, args.quantity)


if __name__ == "__main__":
    main()
