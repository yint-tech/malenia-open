CREATE TABLE `metric_day`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增主建',
    `name`        varchar(64) NOT NULL COMMENT '指标名称',
    `time_key`    varchar(64) NOT NULL COMMENT '时间索引',
    `tags_md5`    varchar(64) NOT NULL COMMENT '对于tag字段自然顺序拼接求md5',
    `tag1`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag2`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag3`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag4`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag5`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `type`        varchar(8)  NOT NULL COMMENT '指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）',
    `value`       double      NOT NULL COMMENT '指标值',
    `create_time` datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_record` (`name`,`tags_md5`,`time_key`),
    KEY           `idx_query` (`name`,`tags_md5`),
    KEY `idx_delete` (`name`,`create_time`) COMMENT '给删除指标使用'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4  COMMENT='监控指标,天级';

CREATE TABLE `metric_hour`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增主建',
    `name`        varchar(64) NOT NULL COMMENT '指标名称',
    `time_key`    varchar(64) NOT NULL COMMENT '时间索引',
    `tags_md5`    varchar(64) NOT NULL COMMENT '对于tag字段自然顺序拼接求md5',
    `tag1`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag2`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag3`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag4`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag5`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `type`        varchar(8)  NOT NULL COMMENT '指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）',
    `value`       double      NOT NULL COMMENT '指标值',
    `create_time` datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_record` (`name`,`tags_md5`,`time_key`),
    KEY           `idx_query` (`name`,`tags_md5`),
    KEY `idx_delete` (`name`,`create_time`) COMMENT '给删除指标使用'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4  COMMENT='监控指标,小时级';

CREATE TABLE `metric_minute`
(
    `id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '自增主建',
    `name`        varchar(64) NOT NULL COMMENT '指标名称',
    `time_key`    varchar(64) NOT NULL COMMENT '时间索引',
    `tags_md5`    varchar(64) NOT NULL COMMENT '对于tag字段自然顺序拼接求md5',
    `tag1`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag2`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag3`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag4`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `tag5`        varchar(64)          DEFAULT NULL COMMENT '指标tag',
    `type`        varchar(8)  NOT NULL COMMENT '指标类型：（counter、gauge、timer，请注意暂时只支持这三种指标）',
    `value`       double      NOT NULL COMMENT '指标值',
    `create_time` datetime             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uniq_record` (`name`,`tags_md5`,`time_key`),
    KEY           `idx_query` (`name`,`tags_md5`),
    KEY `idx_delete` (`name`,`create_time`) COMMENT '给删除指标使用'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4  COMMENT='监控指标,分钟级';
