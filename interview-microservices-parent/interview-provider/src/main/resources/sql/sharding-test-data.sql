-- =============================================
-- ShardingSphere 分片表测试数据
-- 说明：向 2026 年各月份表中插入测试数据
-- =============================================

-- 1 月测试数据
INSERT INTO device_operation_log_202601 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV001', '空压机 A', 1, 380.5, '2026-01-05 08:30:00', '张三', '正常开机'),
       ('DEV002', '冲压机 B', 2, 220.0, '2026-01-10 18:00:00', '李四', '正常关机'),
       ('DEV001', '空压机 A', 3, 450.2, '2026-01-15 14:20:00', '系统', '电压异常告警'),
       ('DEV003', '注塑机 C', 1, 380.0, '2026-01-20 09:00:00', '王五', '维护后开机');

-- 2 月测试数据
INSERT INTO device_operation_log_202602 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV001', '空压机 A', 2, 380.0, '2026-02-05 17:30:00', '张三', '正常关机'),
       ('DEV002', '冲压机 B', 1, 220.5, '2026-02-08 08:15:00', '李四', '春节后开机'),
       ('DEV004', '切割机 D', 4, 0.0, '2026-02-14 10:00:00', '赵六', '定期保养'),
       ('DEV003', '注塑机 C', 3, 410.8, '2026-02-22 16:45:00', '系统', '温度过高');

-- 3 月测试数据
INSERT INTO device_operation_log_202603 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV001', '空压机 A', 1, 385.0, '2026-03-01 08:00:00', '张三', '月度开机检查'),
       ('DEV005', '焊接机器人 E', 1, 380.0, '2026-03-05 09:30:00', '孙七', '新设备投入使用'),
       ('DEV002', '冲压机 B', 4, 0.0, '2026-03-12 14:00:00', '李四', '更换模具保养'),
       ('DEV004', '切割机 D', 2, 220.0, '2026-03-25 18:30:00', '赵六', '下班关机');

-- 4 月测试数据
INSERT INTO device_operation_log_202604 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV003', '注塑机 C', 1, 390.0, '2026-04-02 08:30:00', '王五', '清明节后开机'),
       ('DEV001', '空压机 A', 3, 460.5, '2026-04-10 11:20:00', '系统', '压力异常'),
       ('DEV005', '焊接机器人 E', 2, 380.0, '2026-04-18 17:00:00', '孙七', '正常关机'),
       ('DEV002', '冲压机 B', 1, 225.0, '2026-04-22 08:45:00', '李四', '加班开机');

-- 5 月测试数据
INSERT INTO device_operation_log_202605 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV004', '切割机 D', 1, 220.0, '2026-05-04 09:00:00', '赵六', '劳动节后开机'),
       ('DEV001', '空压机 A', 4, 0.0, '2026-05-10 10:00:00', '张三', '季度保养'),
       ('DEV003', '注塑机 C', 2, 385.0, '2026-05-15 18:00:00', '王五', '正常关机'),
       ('DEV005', '焊接机器人 E', 3, 390.2, '2026-05-20 15:30:00', '系统', '电流波动');

-- 6 月测试数据
INSERT INTO device_operation_log_202606 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV002', '冲压机 B', 1, 220.0, '2026-06-01 08:00:00', '李四', '月初开机'),
       ('DEV004', '切割机 D', 3, 230.5, '2026-06-08 13:40:00', '系统', '电压不稳'),
       ('DEV001', '空压机 A', 2, 380.0, '2026-06-15 17:30:00', '张三', '正常关机'),
       ('DEV003', '注塑机 C', 1, 388.0, '2026-06-22 08:15:00', '王五', '端午节后开机');

-- 7 月测试数据
INSERT INTO device_operation_log_202607 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV005', '焊接机器人 E', 1, 380.0, '2026-07-01 08:30:00', '孙七', '下半年开工'),
       ('DEV002', '冲压机 B', 4, 0.0, '2026-07-10 14:00:00', '李四', '半年保养'),
       ('DEV004', '切割机 D', 2, 220.0, '2026-07-18 18:00:00', '赵六', '正常关机'),
       ('DEV001', '空压机 A', 1, 382.0, '2026-07-25 09:00:00', '张三', '高温天气开机');

-- 8 月测试数据
INSERT INTO device_operation_log_202608 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV003', '注塑机 C', 3, 420.5, '2026-08-05 16:20:00', '系统', '冷却系统故障'),
       ('DEV005', '焊接机器人 E', 2, 380.0, '2026-08-12 17:30:00', '孙七', '正常关机'),
       ('DEV002', '冲压机 B', 1, 222.0, '2026-08-15 08:00:00', '李四', '开机生产'),
       ('DEV004', '切割机 D', 4, 0.0, '2026-08-22 10:30:00', '赵六', '刀片更换保养');

-- 9 月测试数据
INSERT INTO device_operation_log_202609 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV001', '空压机 A', 1, 385.0, '2026-09-01 08:00:00', '张三', '开学季开机'),
       ('DEV003', '注塑机 C', 2, 390.0, '2026-09-08 18:00:00', '王五', '正常关机'),
       ('DEV005', '焊接机器人 E', 3, 395.0, '2026-09-15 14:50:00', '系统', '焊接质量异常'),
       ('DEV002', '冲压机 B', 1, 220.0, '2026-09-22 08:30:00', '李四', '中秋节后开机');

-- 10 月测试数据
INSERT INTO device_operation_log_202610 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV004', '切割机 D', 1, 220.0, '2026-10-08 09:00:00', '赵六', '国庆节后开机'),
       ('DEV001', '空压机 A', 4, 0.0, '2026-10-10 10:00:00', '张三', '秋季保养'),
       ('DEV002', '冲压机 B', 2, 220.0, '2026-10-18 17:30:00', '李四', '正常关机'),
       ('DEV005', '焊接机器人 E', 1, 380.0, '2026-10-25 08:15:00', '孙七', '新订单开机');

-- 11 月测试数据
INSERT INTO device_operation_log_202611 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV003', '注塑机 C', 1, 388.0, '2026-11-01 08:00:00', '王五', '立冬开机'),
       ('DEV004', '切割机 D', 3, 225.0, '2026-11-08 15:10:00', '系统', '电机过载'),
       ('DEV001', '空压机 A', 2, 380.0, '2026-11-15 17:00:00', '张三', '正常关机'),
       ('DEV002', '冲压机 B', 4, 0.0, '2026-11-22 14:00:00', '李四', '年度保养');

-- 12 月测试数据
INSERT INTO device_operation_log_202612 (device_code, device_name, operation_type, operation_value, operation_time,
                                         operator, remark)
VALUES ('DEV005', '焊接机器人 E', 1, 380.0, '2026-12-01 08:30:00', '孙七', '年底赶工开机'),
       ('DEV003', '注塑机 C', 2, 390.0, '2026-12-10 18:00:00', '王五', '正常关机'),
       ('DEV004', '切割机 D', 1, 220.0, '2026-12-15 09:00:00', '赵六', '开机生产'),
       ('DEV001', '空压机 A', 3, 470.0, '2026-12-25 16:30:00', '系统', '圣诞节前电压异常');
