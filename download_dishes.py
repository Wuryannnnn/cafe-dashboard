#!/usr/bin/env python3
import json
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.request import Request, urlopen
from urllib.error import HTTPError, URLError

DISHES = json.loads(Path(__file__).with_name("dishes.json").read_text(encoding="utf-8"))
OUT_DIR = Path(__file__).parent / "meituan_dish_images"
OUT_DIR.mkdir(exist_ok=True)


def safe(s: str) -> str:
    return re.sub(r'[\\/:*?"<>|]', "_", (s or "").strip())[:60]


def ext_from_url(url: str) -> str:
    m = re.search(r"\.([a-zA-Z0-9]{2,4})(?:\?|$)", url)
    return "." + m.group(1).lower() if m else ".jpg"


def fmt_price(cents: int | None) -> str:
    if cents is None:
        return "0"
    yuan = cents / 100
    return f"{int(yuan)}" if yuan == int(yuan) else f"{yuan:g}"


def price_str(d) -> str:
    s, e = d.get("startPrice"), d.get("endPrice")
    if s is None and e is None:
        return "0"
    if s == e or e is None:
        return fmt_price(s)
    return f"{fmt_price(s)}-{fmt_price(e)}"


def download_one(idx: int, dish: dict):
    name = dish["name"]
    img = dish.get("imgUrl")
    if not img:
        return ("skip", idx, name, "no image")
    cat_dir = OUT_DIR / safe(dish.get("firstCategory") or "未分类")
    cat_dir.mkdir(exist_ok=True)
    fname = f"{idx:03d}_{safe(name)}_¥{price_str(dish)}{ext_from_url(img)}"
    path = cat_dir / fname
    if path.exists() and path.stat().st_size > 0:
        return ("exist", idx, name, str(path))
    req = Request(img, headers={"User-Agent": "Mozilla/5.0", "Referer": "https://pos.meituan.com/"})
    try:
        with urlopen(req, timeout=20) as r:
            data = r.read()
        path.write_bytes(data)
        return ("ok", idx, name, str(path))
    except (HTTPError, URLError, TimeoutError) as e:
        return ("fail", idx, name, str(e))


def main():
    ok = fail = skip = exist = 0
    failures = []
    with ThreadPoolExecutor(max_workers=12) as pool:
        futures = [pool.submit(download_one, i + 1, d) for i, d in enumerate(DISHES)]
        for fu in as_completed(futures):
            status, idx, name, info = fu.result()
            if status == "ok":
                ok += 1
            elif status == "exist":
                exist += 1
            elif status == "skip":
                skip += 1
            else:
                fail += 1
                failures.append((idx, name, info))
    print(f"Downloaded: {ok}  Existed: {exist}  No image: {skip}  Failed: {fail}")
    if failures:
        print("\nFailures:")
        for idx, n, e in failures:
            print(f"  #{idx} {n}: {e}")


if __name__ == "__main__":
    main()
