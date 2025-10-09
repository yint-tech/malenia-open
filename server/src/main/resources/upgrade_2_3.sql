alter  table user_info add column  `authentication_real_name` varchar(64) DEFAULT NULL COMMENT '实名认证：真实姓名';
alter  table user_info add column  `authentication_id_card` varchar(64) DEFAULT NULL COMMENT '实名认证：身份证';
alter  table user_info add column  `authentication_company_name` varchar(64) DEFAULT NULL COMMENT '实名认证：企业名称';
alter  table user_info add column  `authentication_license_number` varchar(64) DEFAULT NULL COMMENT '实名认证：营业执照号码';
alter  table user_info add column  `authentication_corporate_name` varchar(64) DEFAULT NULL COMMENT '实名认证：法人姓名';
alter  table user_info add column  `authentication_type` varchar(64) NOT NULL DEFAULT '' COMMENT '实名认证：认证类型,个人,企业';
alter  table user_info add column  `authentication_third_party_id` varchar(64) DEFAULT NULL COMMENT '实名认证：第三方请求id';
alter  table user_info add column  `authentication_comment` varchar(500) DEFAULT NULL COMMENT '实名认证：备注';

alter  table recharge_record add column  `trade_no` varchar(64) DEFAULT NULL COMMENT '支付流水号，支付宝|微信返回';
alter  table recharge_record add UNIQUE KEY `uniq_trade_no` (`trade_no`);