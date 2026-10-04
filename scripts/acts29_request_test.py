#!/usr/bin/env python3
"""Safely inspect or no-op test the Acts29 attendance update request."""

from __future__ import annotations

import argparse
from collections import Counter
from datetime import date as date_type
import hashlib
import json
from pathlib import Path
import re
import sys
from urllib.parse import urlencode, urlparse

from playwright.sync_api import BrowserContext, Frame, Page, sync_playwright


ROOT = Path(__file__).resolve().parents[1]
ENV_PATH = ROOT / ".env"
BASE_URL = "https://acts29.onnuri.or.kr/child_ecare"
LIST_URL = f"{BASE_URL}/child/choolsuk.do?act=choolsukUpdateList"
SAVE_URL = f"{BASE_URL}/child/choolsuk.do?act=choolsuk_update"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Acts29 로그인 후 출석 저장 요청을 검증합니다. 기본 실행은 조회만 하며, "
            "--apply-noop을 지정해야 현재 값을 그대로 POST합니다."
        )
    )
    parser.add_argument("--date", required=True, help="대상 날짜(YYYY-MM-DD)")
    parser.add_argument("--grade", default="전체", help="학년 필터(기본값: 전체)")
    parser.add_argument("--class-name", default="전체", help="반 필터(기본값: 전체)")
    parser.add_argument(
        "--apply-noop",
        action="store_true",
        help="현재 원격 값을 변경 없이 저장 API로 재전송",
    )
    parser.add_argument(
        "--confirm-date",
        help="--apply-noop 사용 시 --date와 동일한 날짜를 다시 입력",
    )
    parser.add_argument("--headed", action="store_true", help="브라우저 창 표시")
    args = parser.parse_args()

    try:
        date_type.fromisoformat(args.date)
    except ValueError as exc:
        parser.error(f"올바르지 않은 날짜입니다: {exc}")

    if args.apply_noop and args.confirm_date != args.date:
        parser.error("실제 요청에는 --confirm-date가 --date와 정확히 같아야 합니다.")
    if not args.apply_noop and args.confirm_date:
        parser.error("--confirm-date는 --apply-noop과 함께 사용해야 합니다.")
    return args


def read_env_value(name: str) -> str:
    if not ENV_PATH.exists():
        return ""
    prefix = f"{name}="
    for raw_line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line.startswith(prefix):
            continue
        value = line[len(prefix) :].strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in ("'", '"'):
            value = value[1:-1]
        return value
    return ""


def target_url(day: str, grade: str, class_name: str) -> str:
    query = urlencode(
        {
            "act": "choolsukUpdate",
            "dlb": grade,
            "soon": class_name,
            "bogoday": day,
        }
    )
    return f"{BASE_URL}/child/choolsuk.do?{query}"


def find_visible(
    context: BrowserContext, selector: str
) -> tuple[Page | None, Frame | None, object | None]:
    for page in context.pages:
        for frame in page.frames:
            locator = frame.locator(selector)
            if locator.count() > 0:
                return page, frame, locator.first
    return None, None, None


def click_first(frame: Frame, selectors: list[str]) -> bool:
    for selector in selectors:
        locator = frame.locator(selector)
        if locator.count() > 0:
            locator.first.click()
            return True
    return False


def authenticate(context: BrowserContext, page: Page, url: str, user_id: str, password: str) -> None:
    page.goto(url, wait_until="domcontentloaded", timeout=60_000)
    page.wait_for_timeout(3_000)

    if urlparse(page.url).hostname == "acts29.onnuri.or.kr" and "choolsuk.do" in page.url:
        return

    email_page, _, email_button = find_visible(
        context, "button:has-text('이메일로 계속하기'):visible"
    )
    if email_button is None:
        raise RuntimeError(f"이메일 로그인 버튼을 찾지 못했습니다: {page.url}")
    email_button.click()
    email_page.wait_for_timeout(1_500)

    login_page, login_frame, username = find_visible(
        context,
        "input[name='userid']:visible, input[name='userId']:visible, "
        "input[name='username']:visible, input[name='email']:visible, "
        "input[type='email']:visible, input[type='text']:visible",
    )
    _, password_frame, password_input = find_visible(context, "input[type='password']:visible")
    if login_frame is None or username is None or password_frame is None or password_input is None:
        raise RuntimeError("통합인증 계정 입력 필드를 찾지 못했습니다.")

    username.fill(user_id)
    password_input.fill(password)
    if not click_first(
        password_frame,
        [
            "button:has-text('로그인'):visible",
            "button[type='submit']:visible",
            "input[type='submit']:visible",
        ],
    ):
        password_input.press("Enter")

    login_page.wait_for_timeout(6_000)
    ncare_page, _, ncare_button = find_visible(context, "button:has-text('엔케어'):visible")
    if ncare_button is None:
        raise RuntimeError(f"엔케어 서비스 버튼을 찾지 못했습니다: {login_page.url}")
    ncare_button.click()
    ncare_page.wait_for_timeout(7_000)

    page = context.pages[-1]
    page.goto(url, wait_until="domcontentloaded", timeout=60_000)
    page.wait_for_timeout(2_000)
    if urlparse(page.url).hostname != "acts29.onnuri.or.kr" or "choolsuk.do" not in page.url:
        raise RuntimeError(f"출석 페이지 인증에 실패했습니다: {page.url}")


def fetch_rows(context: BrowserContext, day: str, grade: str, class_name: str) -> list[dict]:
    response = context.request.post(
        LIST_URL,
        form={"dlb": grade, "soon": class_name, "bogoday": day},
        headers={"X-Requested-With": "XMLHttpRequest"},
        timeout=60_000,
    )
    if not response.ok:
        raise RuntimeError(f"목록 조회 실패: HTTP {response.status}")
    payload = response.json()
    rows = payload.get("list")
    if not isinstance(rows, list):
        raise RuntimeError("목록 응답에 list 배열이 없습니다.")
    return rows


def state_signature(rows: list[dict]) -> list[tuple]:
    return sorted(
        (
            str(row.get("user_id") or ""),
            str(row.get("seq") or ""),
            str(row.get("attend_yn") or ""),
            str(row.get("sau") or ""),
            str(row.get("simbang_yn") or ""),
        )
        for row in rows
    )


def payload_digest(rows: list[dict]) -> str:
    canonical = json.dumps(rows, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()[:16]


def main() -> int:
    args = parse_args()
    user_id = read_env_value("ACTS29_USER_ID")
    password = read_env_value("ACTS29_PASSWORD")
    if not user_id or not password:
        print(".env의 ACTS29_USER_ID/ACTS29_PASSWORD가 비어 있습니다.", file=sys.stderr)
        return 2

    url = target_url(args.date, args.grade, args.class_name)
    with sync_playwright() as pw:
        browser = pw.chromium.launch(channel="chrome", headless=not args.headed)
        context = browser.new_context(locale="ko-KR", timezone_id="Asia/Seoul")
        page = context.new_page()
        authenticate(context, page, url, user_id, password)

        rows_before = fetch_rows(context, args.date, args.grade, args.class_name)
        counts = Counter(str(row.get("attend_yn") or "") for row in rows_before)
        birth_lengths = Counter(
            len(re.sub(r"[^0-9]", "", str(row.get("birth") or "")))
            for row in rows_before
        )
        print(f"date={args.date}")
        print(f"rows={len(rows_before)}")
        print(f"attendance_counts={dict(sorted(counts.items()))}")
        print(f"birth_digit_lengths={dict(sorted(birth_lengths.items()))}")
        print(f"payload_digest={payload_digest(rows_before)}")
        print("save_fields=bogoday,dlb,list,sau_arr,soon")

        if not args.apply_noop:
            print("result=DRY_RUN (저장 요청을 보내지 않았습니다)")
            context.close()
            browser.close()
            return 0

        reasons = [str(row.get("sau") or "") for row in rows_before]
        if any(":" in reason for reason in reasons):
            raise RuntimeError("결석 사유에 ':' 문자가 있어 안전하게 재전송할 수 없습니다.")

        response = context.request.post(
            SAVE_URL,
            form={
                "list": json.dumps(rows_before, ensure_ascii=False, separators=(",", ":")),
                "sau_arr": ":".join(reasons),
                "bogoday": args.date,
                "dlb": args.grade,
                "soon": args.class_name,
            },
            headers={"X-Requested-With": "XMLHttpRequest"},
            timeout=60_000,
        )
        print(f"save_http_status={response.status}")
        if not response.ok:
            raise RuntimeError(f"저장 요청 실패: HTTP {response.status}")

        rows_after = fetch_rows(context, args.date, args.grade, args.class_name)
        unchanged = state_signature(rows_before) == state_signature(rows_after)
        print(f"verified_unchanged={str(unchanged).lower()}")
        print("result=NOOP_POST_OK" if unchanged else "result=NOOP_POST_CHANGED_REMOTE_STATE")

        context.close()
        browser.close()
        return 0 if unchanged else 3


if __name__ == "__main__":
    raise SystemExit(main())
