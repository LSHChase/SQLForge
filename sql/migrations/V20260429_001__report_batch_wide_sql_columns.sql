ALTER TABLE report_batch_item
  ADD COLUMN sql_column_name VARCHAR(128) DEFAULT NULL COMMENT 'Source spreadsheet column that supplied this SQL' AFTER source_file_line,
  ADD COLUMN sql_ordinal_in_report INT DEFAULT NULL COMMENT '1-based SQL ordinal inside the same report_code' AFTER sql_column_name,
  ADD KEY idx_report_batch_item_report_sql (batch_id, report_code, sql_ordinal_in_report);
