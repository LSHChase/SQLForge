#!/usr/bin/env python3
"""Verify Java quality scan reports produced by Maven PMD and Checkstyle."""

from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
POM_PATH = ROOT / "pom.xml"
NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}


def load_modules() -> list[str]:
    tree = ET.parse(POM_PATH)
    root = tree.getroot()
    modules = root.findall("m:modules/m:module", NAMESPACE)
    if not modules:
        modules = root.findall("modules/module")
    return [module.text.strip() for module in modules if module.text and module.text.strip()]


def main() -> int:
    modules = load_modules()
    if not modules:
        print("No Maven modules found in pom.xml; nothing to verify.")
        return 0

    expected = (
        ("target/pmd.xml", "PMD XML report"),
        ("target/site/pmd.html", "PMD HTML report"),
        ("target/checkstyle-result.xml", "Checkstyle XML report"),
        ("target/site/checkstyle.html", "Checkstyle HTML report"),
    )

    missing: list[str] = []
    verified: list[str] = []
    for module in modules:
        module_dir = ROOT / module
        if not module_dir.exists():
            missing.append(f"{module}: module directory is missing")
            continue
        for relative_path, label in expected:
            report_path = module_dir / relative_path
            if report_path.exists():
                verified.append(f"{module}: {label} -> {report_path.relative_to(ROOT)}")
            else:
                missing.append(f"{module}: missing {label} ({report_path.relative_to(ROOT)})")

    if missing:
        print("Java quality report verification failed:")
        for item in missing:
            print(f"- {item}")
        return 1

    print("Java quality report verification passed:")
    for item in verified:
        print(f"- {item}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
