#!/usr/bin/env python3
"""Generate benchmark result charts for online-shop README."""

import json
import os
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

# Prefer a Chinese-capable sans on Windows; fall back to system sans
plt.rcParams["font.family"] = ["Microsoft YaHei", "SimHei", "Segoe UI", "system-ui", "sans-serif"]
plt.rcParams["axes.unicode_minus"] = False

# Validated categorical palette (light mode) from dataviz skill
PALETTE = {
    "blue": "#2a78d6",
    "aqua": "#1baf7a",
    "yellow": "#eda100",
    "green": "#008300",
    "violet": "#4a3aa7",
    "red": "#e34948",
    "magenta": "#e87ba4",
    "orange": "#eb6834",
}

# Chart chrome for light surface
SURFACE = "#fcfcfb"
PRIMARY_INK = "#0b0b0b"
SECONDARY_INK = "#52514e"
GRIDLINE = "#e1e0d9"
BASELINE = "#c3c2b7"

RESULTS_DIR = Path(__file__).resolve().parent / "results"
OUTPUT_DIR = Path(__file__).resolve().parent.parent.parent / "docs" / "images"


def load_json(path: Path) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def setup_ax(ax: plt.Axes, title: str, ylabel: str) -> None:
    """Apply shared chart chrome."""
    ax.set_title(title, fontsize=12, fontweight="semibold", color=PRIMARY_INK, pad=12)
    ax.set_ylabel(ylabel, fontsize=10, color=SECONDARY_INK)
    ax.set_facecolor(SURFACE)
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_color(BASELINE)
    ax.spines["bottom"].set_color(BASELINE)
    ax.tick_params(axis="both", colors=SECONDARY_INK, labelsize=9)
    ax.yaxis.grid(True, color=GRIDLINE, linestyle="-", linewidth=0.8)
    ax.set_axisbelow(True)


def plot_grouped_bars(ax: plt.Axes, labels: list[str], series: dict[str, list[float]],
                      ylabel: str, title: str, show_legend: bool = True) -> None:
    """Render a grouped bar chart with direct labels."""
    x = np.arange(len(labels))
    width = 0.28
    gap = 0.02  # surface gap between adjacent bars
    n = len(series)
    offsets = np.linspace(-(n - 1) / 2, (n - 1) / 2, n) * (width + gap)

    for offset, (name, values), color in zip(offsets, series.items(), list(PALETTE.values())):
        bars = ax.bar(x + offset, values, width, label=name, color=color, edgecolor="none")
        # Direct labels at bar tips
        for bar in bars:
            height = bar.get_height()
            if np.isfinite(height):
                ax.annotate(
                    f"{height:.1f}" if height < 100 else f"{int(height)}",
                    xy=(bar.get_x() + bar.get_width() / 2, height),
                    xytext=(0, 4),
                    textcoords="offset points",
                    ha="center", va="bottom",
                    fontsize=8, color=SECONDARY_INK,
                )

    setup_ax(ax, title, ylabel)
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=10, color=PRIMARY_INK)
    if show_legend:
        ax.legend(frameon=False, fontsize=9, loc="upper right", labelcolor=PRIMARY_INK)


def generate_cache_comparison(summary: dict, output_path: Path) -> None:
    """Cold vs Warm cache grouped bar chart (small multiples for different scales)."""
    cold = summary.get("product_detail_cold", {})
    warm = summary.get("product_detail_warm", {})

    fig, axes = plt.subplots(1, 3, figsize=(14, 4.2), facecolor=SURFACE)
    fig.suptitle(
        "商品详情接口：Cold Cache vs Warm Cache",
        fontsize=14, fontweight="bold", color=PRIMARY_INK, y=1.08,
    )

    series = {"Cold Cache": [], "Warm Cache": []}
    labels = ["TPS", "Avg(ms)", "P95(ms)", "P99(ms)"]
    cold_vals = [cold.get("tps", 0), cold.get("avg_latency_ms", 0),
                 cold.get("p95_latency_ms", 0), cold.get("p99_latency_ms", 0)]
    warm_vals = [warm.get("tps", 0), warm.get("avg_latency_ms", 0),
                 warm.get("p95_latency_ms", 0), warm.get("p99_latency_ms", 0)]

    # Subplot 1: TPS
    tps_bars = plot_grouped_bars(
        axes[0], ["Throughput"],
        {"Cold Cache": [cold_vals[0]], "Warm Cache": [warm_vals[0]]},
        "Requests / sec", "吞吐量 TPS", show_legend=False,
    )

    # Subplot 2: Latency (avg / p95 / p99)
    latency_labels = ["Avg", "P95", "P99"]
    plot_grouped_bars(
        axes[1], latency_labels,
        {"Cold Cache": cold_vals[1:], "Warm Cache": warm_vals[1:]},
        "Latency (ms)", "响应延迟", show_legend=False,
    )

    # Subplot 3: DB Select delta
    db_delta = [cold.get("db_select_delta", 0), warm.get("db_select_delta", 0)]
    x = np.arange(2)
    width = 0.45
    colors = [PALETTE["blue"], PALETTE["aqua"]]
    bars = axes[2].bar(x, db_delta, width, color=colors, edgecolor="none")
    for bar in bars:
        height = bar.get_height()
        axes[2].annotate(
            f"{int(height)}",
            xy=(bar.get_x() + bar.get_width() / 2, height),
            xytext=(0, 4),
            textcoords="offset points",
            ha="center", va="bottom",
            fontsize=10, color=PRIMARY_INK, fontweight="semibold",
        )
    setup_ax(axes[2], "MySQL Com_select 增长", "SELECT 增长数")
    axes[2].set_xticks(x)
    axes[2].set_xticklabels(["Cold Cache", "Warm Cache"], fontsize=10, color=PRIMARY_INK)

    # Shared figure legend at top to avoid overlapping bars
    handles = [axes[0].containers[0][0], axes[0].containers[1][0]]
    labels = ["Cold Cache", "Warm Cache"]
    fig.legend(
        handles, labels, loc="upper center", ncol=2, frameon=False,
        fontsize=10, labelcolor=PRIMARY_INK, bbox_to_anchor=(0.5, 1.02),
    )

    plt.tight_layout()
    fig.savefig(output_path, dpi=150, bbox_inches="tight", facecolor=SURFACE)
    plt.close(fig)
    print(f"[chart] {output_path}")


def generate_order_create_latency(summary: dict, output_path: Path) -> None:
    """Order create latency profile as a single-series column chart."""
    order = summary.get("order_create", {})
    labels = ["Avg", "P50", "P95", "P99"]
    values = [
        order.get("avg_latency_ms", 0),
        order.get("p50_latency_ms", 0),
        order.get("p95_latency_ms", 0),
        order.get("p99_latency_ms", 0),
    ]

    fig, ax = plt.subplots(figsize=(7, 4.5), facecolor=SURFACE)
    x = np.arange(len(labels))
    width = 0.45
    bars = ax.bar(x, values, width, color=PALETTE["blue"], edgecolor="none")

    for bar in bars:
        height = bar.get_height()
        ax.annotate(
            f"{height:.1f}",
            xy=(bar.get_x() + bar.get_width() / 2, height),
            xytext=(0, 4),
            textcoords="offset points",
            ha="center", va="bottom",
            fontsize=10, color=PRIMARY_INK, fontweight="semibold",
        )

    setup_ax(ax, "下单链路响应延迟", "Latency (ms)")
    ax.set_xticks(x)
    ax.set_xticklabels(labels, fontsize=11, color=PRIMARY_INK)
    ax.set_ylim(0, max(values) * 1.15)

    fig.savefig(output_path, dpi=150, bbox_inches="tight", facecolor=SURFACE)
    plt.close(fig)
    print(f"[chart] {output_path}")


def generate_four_layer_defense(output_path: Path) -> None:
    """Baseline vs Layer 1-4: TPS and P99 latency as grouped bars."""
    layers = ["Baseline", "Layer 1", "Layer 2", "Layer 3", "Layer 4"]
    files = ["baseline.json", "layer1.json", "layer2.json", "layer3.json", "layer4.json"]
    tps_values = []
    p99_values = []

    for filename in files:
        data = load_json(RESULTS_DIR / filename)
        tps_values.append(data.get("tps", 0))
        p99_values.append(data.get("p99_latency_ms", 0))

    fig, axes = plt.subplots(1, 2, figsize=(13, 4.2), facecolor=SURFACE)
    fig.suptitle(
        "高并发四层防御：下单链路演进对比",
        fontsize=14, fontweight="bold", color=PRIMARY_INK, y=1.02,
    )

    x = np.arange(len(layers))
    width = 0.5
    colors = [PALETTE["blue"], PALETTE["aqua"], PALETTE["yellow"], PALETTE["green"], PALETTE["violet"]]

    # TPS
    bars = axes[0].bar(x, tps_values, width, color=colors, edgecolor="none")
    for bar in bars:
        height = bar.get_height()
        axes[0].annotate(
            f"{height:.1f}",
            xy=(bar.get_x() + bar.get_width() / 2, height),
            xytext=(0, 4),
            textcoords="offset points",
            ha="center", va="bottom",
            fontsize=9, color=PRIMARY_INK, fontweight="semibold",
        )
    setup_ax(axes[0], "吞吐量 TPS", "Requests / sec")
    axes[0].set_xticks(x)
    axes[0].set_xticklabels(layers, fontsize=9, color=PRIMARY_INK)
    axes[0].set_ylim(0, max(tps_values) * 1.15)

    # P99 Latency
    bars = axes[1].bar(x, p99_values, width, color=colors, edgecolor="none")
    for bar in bars:
        height = bar.get_height()
        axes[1].annotate(
            f"{int(height)}",
            xy=(bar.get_x() + bar.get_width() / 2, height),
            xytext=(0, 4),
            textcoords="offset points",
            ha="center", va="bottom",
            fontsize=9, color=PRIMARY_INK, fontweight="semibold",
        )
    setup_ax(axes[1], "P99 延迟", "Latency (ms)")
    axes[1].set_xticks(x)
    axes[1].set_xticklabels(layers, fontsize=9, color=PRIMARY_INK)
    axes[1].set_ylim(0, max(p99_values) * 1.15)

    plt.tight_layout()
    fig.savefig(output_path, dpi=150, bbox_inches="tight", facecolor=SURFACE)
    plt.close(fig)
    print(f"[chart] {output_path}")


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    summary = load_json(RESULTS_DIR / "jmeter_summary.json")

    generate_cache_comparison(summary, OUTPUT_DIR / "cache_comparison.png")
    generate_order_create_latency(summary, OUTPUT_DIR / "order_create_latency.png")
    generate_four_layer_defense(OUTPUT_DIR / "four_layer_defense.png")

    print(f"[done] all charts saved to {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
