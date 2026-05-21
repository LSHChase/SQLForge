DROP PROCEDURE IF EXISTS add_benchmark_task_scale_target_if_missing;

DELIMITER $$

CREATE PROCEDURE add_benchmark_task_scale_target_if_missing()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'benchmark_task'
          AND column_name = 'scale_target_json'
    ) THEN
        ALTER TABLE benchmark_task
            ADD COLUMN scale_target_json JSON DEFAULT NULL COMMENT '生产规模目标与未验证证据边界 JSON' AFTER dataset_size_label;
    END IF;
END$$

DELIMITER ;

CALL add_benchmark_task_scale_target_if_missing();

DROP PROCEDURE IF EXISTS add_benchmark_task_scale_target_if_missing;
