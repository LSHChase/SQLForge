#!/usr/bin/env python3
"""Validate real Kafka runtime bootstrap and security parameters for Phase-F gates."""

from __future__ import annotations

import argparse
import os
import sys


ALLOWED_PROTOCOLS = {"PLAINTEXT", "SSL", "SASL_PLAINTEXT", "SASL_SSL"}


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Validate Kafka runtime bootstrap/security configuration.")
    parser.add_argument(
        "--bootstrap-servers",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_BOOTSTRAP", ""),
        help="Kafka bootstrap servers. Defaults to SQLFORGE_PROD_KAFKA_BOOTSTRAP.",
    )
    parser.add_argument(
        "--security-protocol",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_SECURITY_PROTOCOL", "PLAINTEXT"),
        help="Kafka security protocol. Defaults to SQLFORGE_PROD_KAFKA_SECURITY_PROTOCOL or PLAINTEXT.",
    )
    parser.add_argument(
        "--sasl-mechanism",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_SASL_MECHANISM", ""),
        help="Kafka SASL mechanism.",
    )
    parser.add_argument(
        "--sasl-jaas-config",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_SASL_JAAS_CONFIG", ""),
        help="Kafka SASL JAAS configuration.",
    )
    parser.add_argument(
        "--ssl-truststore-location",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_LOCATION", ""),
        help="Kafka SSL truststore location.",
    )
    parser.add_argument(
        "--ssl-truststore-password",
        default=os.environ.get("SQLFORGE_PROD_KAFKA_SSL_TRUSTSTORE_PASSWORD", ""),
        help="Kafka SSL truststore password.",
    )
    return parser


def main() -> int:
    args = build_parser().parse_args()
    errors: list[str] = []
    bootstrap_servers = args.bootstrap_servers.strip()
    security_protocol = args.security_protocol.strip().upper() or "PLAINTEXT"

    if not bootstrap_servers:
        errors.append("bootstrap servers must not be empty")
    if security_protocol not in ALLOWED_PROTOCOLS:
        errors.append(
            "security protocol must be one of: " + ", ".join(sorted(ALLOWED_PROTOCOLS))
        )
    if security_protocol.startswith("SASL_"):
        if not args.sasl_mechanism.strip():
            errors.append("SASL mode requires sasl mechanism")
        if not args.sasl_jaas_config.strip():
            errors.append("SASL mode requires sasl JAAS configuration")
    if "SSL" in security_protocol:
        if not args.ssl_truststore_location.strip():
            errors.append("SSL mode requires truststore location")
        if not args.ssl_truststore_password.strip():
            errors.append("SSL mode requires truststore password")

    if errors:
        sys.stderr.write("Kafka runtime configuration verification failed:\n")
        for item in errors:
            sys.stderr.write(f"- {item}\n")
        return 1

    sys.stdout.write(
        "Kafka runtime configuration verification passed: "
        f"bootstrap={bootstrap_servers}, securityProtocol={security_protocol}.\n"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
