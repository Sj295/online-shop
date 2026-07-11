import json, requests, time, os

BASE = "http://localhost:8080"
USERS_FILE = os.path.join(os.path.dirname(__file__), "results", "bench_users.json")

with open(USERS_FILE, encoding="utf-8") as f:
    mapping = json.load(f)

username = "bench_0001"
info = mapping[username]
resp = requests.post(f"{BASE}/api/user/login", json={"username": username, "password": "123456"}, timeout=10)
token = resp.json()["data"]

r = requests.post(f"{BASE}/api/order/create", headers={"Authorization": token},
                  json={"addressId": info["address_id"], "remark": "async-test"}, timeout=10)
print("create:", r.json())
order_no = r.json().get("data", {}).get("orderNo")

for i in range(30):
    s = requests.get(f"{BASE}/api/order/create/status", headers={"Authorization": token},
                     params={"orderNo": order_no}, timeout=10)
    data = s.json().get("data", {})
    print(f"poll {i+1}:", data)
    if data.get("status") in ("SUCCESS", "FAILED"):
        break
    time.sleep(0.5)
