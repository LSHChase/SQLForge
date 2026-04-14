# SQLForge Backend

Java 8 + Spring Boot backend scaffold for the SQLForge split architecture.

## Run

```bash
mvn spring-boot:run
```

## API

- `GET /api/v1/system/health`
- `GET /api/v1/system/engines`

## Notes

- text encoding: UTF-8
- line ending: LF
- runtime target: `amd64` and `arm64`
- connector support baseline: MySQL, Trino, Presto, ClickHouse, MRS-Hetu, Kyligence
