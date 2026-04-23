#!/usr/bin/env python3

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


class MockHetuHandler(BaseHTTPRequestHandler):
    server_version = "SQLForgeMockHetu/1.0"

    def do_GET(self):
        if self.path == "/health":
            self._write_json(200, {"status": "UP"})
            return
        if self.path == "/v1/statement/query-1/next":
            self._write_json(
                200,
                {
                    "id": "query-1",
                    "columns": [{"name": "order_id"}, {"name": "state"}],
                    "data": [[2, "DONE"]],
                    "stats": {"elapsedTimeMillis": 52, "processedRows": 128},
                },
            )
            return
        self._write_json(404, {"error": "not found", "path": self.path})

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(content_length).decode("utf-8")
        if self.path == "/query":
            self._write_json(
                200,
                {
                    "rows": [{"order_id": 1, "state": "READY"}],
                    "elapsedMs": 41,
                    "scannedRows": 128,
                    "cacheHit": False,
                    "sqlText": body,
                },
            )
            return
        if self.path == "/v1/statement":
            host = self.headers.get("Host", f"127.0.0.1:{self.server.server_address[1]}")
            self._write_json(
                200,
                {
                    "id": "query-1",
                    "columns": [{"name": "order_id"}, {"name": "state"}],
                    "data": [[1, "RUNNING"]],
                    "stats": {"elapsedTimeMillis": 31, "processedRows": 64},
                    "nextUri": f"http://{host}/v1/statement/query-1/next",
                    "receivedSql": body,
                },
            )
            return
        self._write_json(404, {"error": "not found", "path": self.path})

    def log_message(self, format, *args):
        return

    def _write_json(self, status_code, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main():
    parser = argparse.ArgumentParser(description="Run a minimal mock Hetu coordinator for runtime smoke.")
    parser.add_argument("--port", type=int, default=18080)
    args = parser.parse_args()

    server = ThreadingHTTPServer(("127.0.0.1", args.port), MockHetuHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
