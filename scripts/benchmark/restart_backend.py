#!/usr/bin/env python3
"""Helper to restart the local shop-portal backend for cold-cache benchmark."""

import argparse
import os
import platform
import shutil
import subprocess
import sys
import time
import urllib.request


def find_pid() -> int | None:
    """Find PID of the shop-portal Java process by listening port."""
    system = platform.system()
    port = 8080
    if system == "Windows":
        try:
            result = subprocess.run(
                ["netstat", "-ano"],
                capture_output=True, text=True, check=True
            )
            for line in result.stdout.splitlines():
                if f":{port}" in line and "LISTENING" in line:
                    parts = line.strip().split()
                    if parts:
                        try:
                            return int(parts[-1])
                        except ValueError:
                            continue
        except Exception:
            pass
    else:
        try:
            result = subprocess.run(
                ["lsof", "-ti", f"tcp:{port}"],
                capture_output=True, text=True, check=True
            )
            return int(result.stdout.strip().splitlines()[0])
        except Exception:
            pass
    return None


def kill_backend() -> None:
    """Kill the running shop-portal process."""
    pid = find_pid()
    if pid:
        print(f"[restart] killing shop-portal process {pid}")
        try:
            if platform.system() == "Windows":
                subprocess.run(["taskkill", "/PID", str(pid), "/F"], check=True)
            else:
                os.kill(pid, 9)
        except Exception as e:
            print(f"[warn] failed to kill process {pid}: {e}")
    else:
        print("[restart] no running shop-portal process found")
    # Give the OS time to release the port
    time.sleep(3)


def flush_redis() -> None:
    """Flush Redis DB."""
    redis_cli_candidates = ["redis-cli"]
    if platform.system() == "Windows":
        redis_cli_candidates.insert(0, r"C:\develop\Redis-x64-3.2.100\redis-cli.exe")

    for cli in redis_cli_candidates:
        try:
            subprocess.run([cli, "FLUSHDB"], check=True, capture_output=True)
            print(f"[restart] flushed Redis via {cli}")
            return
        except FileNotFoundError:
            continue
        except subprocess.CalledProcessError as e:
            print(f"[warn] {cli} FLUSHDB failed: {e}")
            return

    try:
        import redis
        r = redis.Redis(host="localhost", port=6379, db=0, protocol=2)
        r.flushdb()
        print("[restart] flushed Redis via Python redis client")
    except ImportError:
        print("[warn] redis-cli and Python redis client not found, skipping Redis flush")
    except Exception as e:
        print(f"[warn] failed to flush Redis: {e}")


def start_backend(project_root: str) -> int:
    """Start shop-portal in the background and return its shell PID."""
    portal_dir = os.path.join(project_root, "backend", "shop-portal")
    log_file = os.path.join(portal_dir, "target", "portal.log")
    os.makedirs(os.path.dirname(log_file), exist_ok=True)

    print(f"[restart] starting shop-portal from {portal_dir}")
    log = open(log_file, "w")
    mvn = shutil.which("mvn") or "mvn"
    if platform.system() == "Windows":
        proc = subprocess.Popen(
            [mvn, "spring-boot:run", "-DskipTests"],
            cwd=portal_dir,
            stdout=log,
            stderr=subprocess.STDOUT,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP,
        )
    else:
        proc = subprocess.Popen(
            [mvn, "spring-boot:run", "-DskipTests"],
            cwd=portal_dir,
            stdout=log,
            stderr=subprocess.STDOUT,
        )
    return proc.pid


def wait_for_backend(url: str = "http://localhost:8080/api/product/13", timeout: int = 120) -> None:
    """Wait until the backend is ready."""
    print(f"[restart] waiting for backend at {url} ...")
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            with urllib.request.urlopen(url, timeout=2) as resp:
                if resp.status == 200:
                    data = resp.read().decode("utf-8")
                    if '"code":200' in data:
                        print("[restart] backend is ready")
                        return
        except Exception:
            pass
        time.sleep(2)
    raise RuntimeError("backend did not become ready in time")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--project-root", default=os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..")),
                        help="path to online-shop project root")
    args = parser.parse_args()

    kill_backend()
    flush_redis()
    start_backend(args.project_root)
    wait_for_backend()


if __name__ == "__main__":
    main()
