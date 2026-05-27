"""SQLForge 北京时间工具。"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

BEIJING_TIMEZONE = timezone(timedelta(hours=8), "Asia/Shanghai")


def now_beijing() -> datetime:
    return datetime.now(BEIJING_TIMEZONE)


def now_beijing_iso() -> str:
    return now_beijing().isoformat(timespec="seconds")


def now_beijing_compact() -> str:
    return now_beijing().strftime("%Y%m%d%H%M%S")


def today_beijing_iso() -> str:
    return now_beijing().date().isoformat()
