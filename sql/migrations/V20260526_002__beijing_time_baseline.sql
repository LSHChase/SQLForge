SET time_zone = '+08:00';

-- SQLForge 时间字段统一按北京时间（Asia/Shanghai）管理和展示。
-- DATETIME/CURRENT_TIMESTAMP 字段依赖连接会话时区；应用 JDBC、compose 与脚本入口同步固定为 +08:00。
