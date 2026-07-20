"""
기존에 만들어둔 온기카페 CSV 데이터(cafe_expense_receipts.csv, cafe_expense_items.csv,
cafe_budget.csv)를 실제 백엔드(Spring Boot) API로 밀어넣는 일괄 등록 스크립트.

주의: 이 스크립트는 OCR을 거치지 않는다. 이미 구조화되어있는 CSV 데이터를 그대로
POST /api/receipts, POST /api/budgets 에 반복 호출해서 저장하는 것뿐이라, PaddleOCR과
무관하게 빠르게 끝난다 (영수증 300여 건 기준 1~2분 이내).

사용법:
    pip install requests
    python import_csv_to_backend.py --data-dir cafe_synthetic_data --api-base http://localhost:8080 --store-id 1
"""

import argparse
import csv
import sys
from collections import defaultdict

try:
    import requests
except ImportError:
    print("requests 패키지가 필요합니다: pip install requests")
    sys.exit(1)


def load_receipts_with_items(data_dir: str, store_id: int):
    receipts = {}
    with open(f"{data_dir}/cafe_expense_receipts.csv", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            receipts[row["ReceiptID"]] = {
                "storeId": store_id,
                "documentType": "RECEIPT",
                "storeName": row["VendorName"],
                "businessNumber": None,
                "transactionDate": row["TransactionDate"],
                "transactionTime": row["TransactionTime"] or None,
                "paymentMethod": row["PaymentMethod"],
                "items": [],
                "supplyAmount": int(row["SupplyAmount"]) if row["SupplyAmount"] else None,
                "vat": int(row["Vat"]) if row["Vat"] else None,
                "taxFreeAmount": int(row["TaxFreeAmount"]) if row["TaxFreeAmount"] else 0,
                "totalAmount": int(row["TotalAmount"]),
                "category": row["Category"],
                "force": True,  # 과거 데이터 일괄 적재이므로 중복 의심 경고 없이 진행
            }

    with open(f"{data_dir}/cafe_expense_items.csv", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            receipt = receipts.get(row["ReceiptID"])
            if receipt is None:
                continue
            receipt["items"].append({
                "itemName": row["ItemName"],
                "quantity": int(row["Quantity"]) if row["Quantity"] else 1,
                "unit": row["Unit"] or None,
                "unitPrice": int(row["UnitPrice"]) if row["UnitPrice"] else None,
                "totalPrice": int(row["TotalPrice"]),
            })

    return list(receipts.values())


def load_budgets(data_dir: str, store_id: int):
    budgets = []
    with open(f"{data_dir}/cafe_budget.csv", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            budgets.append({
                "storeId": store_id,
                "yearMonth": row["YearMonth"],
                "category": row["Category"],
                "budgetAmount": int(row["BudgetAmount"]),
            })
    return budgets


def import_receipts(api_base: str, receipts: list) -> tuple[int, int]:
    ok, fail = 0, 0
    for i, receipt in enumerate(receipts, 1):
        try:
            resp = requests.post(f"{api_base}/api/receipts", json=receipt, timeout=10)
            if resp.status_code in (200, 201):
                ok += 1
            else:
                fail += 1
                print(f"  [실패 {i}/{len(receipts)}] {receipt['transactionDate']} "
                      f"{receipt['storeName']} - status={resp.status_code} body={resp.text[:200]}")
        except requests.RequestException as e:
            fail += 1
            print(f"  [예외 {i}/{len(receipts)}] {e}")

        if i % 50 == 0:
            print(f"  ... {i}/{len(receipts)}건 처리 중 (성공 {ok}, 실패 {fail})")

    return ok, fail


def import_budgets(api_base: str, budgets: list) -> tuple[int, int]:
    ok, fail = 0, 0
    for i, budget in enumerate(budgets, 1):
        try:
            resp = requests.post(f"{api_base}/api/budgets", json=budget, timeout=10)
            if resp.status_code in (200, 201):
                ok += 1
            else:
                fail += 1
                print(f"  [실패 {i}/{len(budgets)}] {budget['yearMonth']} "
                      f"{budget['category']} - status={resp.status_code} body={resp.text[:200]}")
        except requests.RequestException as e:
            fail += 1
            print(f"  [예외 {i}/{len(budgets)}] {e}")

    return ok, fail


def main():
    parser = argparse.ArgumentParser(description="온기카페 CSV 데이터를 백엔드 API로 일괄 등록")
    parser.add_argument("--data-dir", default="cafe_synthetic_data")
    parser.add_argument("--api-base", default="http://localhost:8080")
    parser.add_argument("--store-id", type=int, default=1)
    args = parser.parse_args()

    print(f"'{args.data_dir}' 폴더에서 데이터를 읽어 '{args.api_base}' 로 등록합니다.\n")

    receipts = load_receipts_with_items(args.data_dir, args.store_id)
    budgets = load_budgets(args.data_dir, args.store_id)

    print(f"영수증 {len(receipts)}건 등록 시작...")
    r_ok, r_fail = import_receipts(args.api_base, receipts)
    print(f"영수증 등록 완료: 성공 {r_ok}건 / 실패 {r_fail}건\n")

    print(f"예산 {len(budgets)}건 등록 시작...")
    b_ok, b_fail = import_budgets(args.api_base, budgets)
    print(f"예산 등록 완료: 성공 {b_ok}건 / 실패 {b_fail}건")


if __name__ == "__main__":
    main()
