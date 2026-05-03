#!/usr/bin/env python3
"""读 dishes.json, 生成可直接拼到 data-local.sql 的分类+商品 INSERT 语句."""
import json
from pathlib import Path

ROOT = Path(__file__).parent
DISHES = json.loads((ROOT / "dishes.json").read_text(encoding="utf-8"))
OUT = ROOT / "src/main/resources/seed_meituan_products.sql"

# 一级分类 → (category_id, print_station). 出现顺序保留美团原顺序.
KITCHEN_CATS = {"沙拉类", "主食类", "三明治类", "牛扒类", "小吃类", "甜品类", "面包",
                "加煎蛋（红心鸡蛋）", "餐盒", "单人套餐", "团建套餐"}

categories = []  # ordered unique
seen = set()
for d in DISHES:
    c = d["firstCategory"] or "未分类"
    if c not in seen:
        seen.add(c)
        categories.append(c)

cat_id_of = {c: i + 1 for i, c in enumerate(categories)}


def sql_str(s: str) -> str:
    return "'" + (s or "").replace("'", "''") + "'"


def yuan(cents):
    return f"{cents / 100:.2f}" if cents else "0.00"


lines = [
    "-- ========================================================",
    "-- 美团菜单复刻: 18 个分类 + 128 个商品 (autogen, do not edit)",
    "-- 来源: dishes.json (从美团管家 API 抓取)",
    "-- ========================================================",
    "",
    "-- 类目",
]
for c in categories:
    cid = cat_id_of[c]
    print_station = 1 if c in KITCHEN_CATS else 0
    lines.append(
        f"INSERT INTO product_category (category_id, category_name, category_type, print_station) "
        f"VALUES ({cid}, {sql_str(c)}, {cid}, {print_station});"
    )

lines += ["", "-- 商品 (128)"]
for i, d in enumerate(DISHES, 1):
    pid = f"mt{i:03d}"
    cat_type = cat_id_of[d["firstCategory"] or "未分类"]
    img = d.get("imgUrl") or ""
    desc = ""
    if d["startPrice"] != d["endPrice"]:
        desc = f"价格区间 ¥{yuan(d['startPrice'])} - ¥{yuan(d['endPrice'])}"
    lines.append(
        f"INSERT INTO product_info (product_id, product_name, product_price, product_stock, "
        f"product_description, product_icon, product_status, category_type, sort_order) "
        f"VALUES ({sql_str(pid)}, {sql_str(d['name'])}, {yuan(d['startPrice'])}, 999, "
        f"{sql_str(desc)}, {sql_str(img)}, 0, {cat_type}, {i});"
    )

lines.append("")
OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Wrote {OUT}  ({len(categories)} categories, {len(DISHES)} products)")
