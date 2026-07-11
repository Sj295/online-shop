SET NAMES utf8mb4;
USE online_shop;

-- 商品/SKU 乐观锁版本号
ALTER TABLE pms_product ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE pms_sku ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- 订单/订单项常用查询索引
ALTER TABLE oms_order ADD INDEX idx_create_time (create_time);
ALTER TABLE oms_order_item ADD INDEX idx_product_id (product_id);
ALTER TABLE oms_order_item ADD INDEX idx_sku_id (sku_id);

-- 订单归档表
CREATE TABLE IF NOT EXISTS oms_order_archive (
    id BIGINT PRIMARY KEY COMMENT '原订单ID',
    order_no VARCHAR(64) NOT NULL COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    pay_amount DECIMAL(10, 2) NOT NULL COMMENT '应付金额',
    freight_amount DECIMAL(10, 2) DEFAULT 0.00 COMMENT '运费',
    status TINYINT DEFAULT 0 COMMENT '订单状态',
    receiver_name VARCHAR(64) DEFAULT NULL COMMENT '收货人',
    receiver_phone VARCHAR(20) DEFAULT NULL COMMENT '收货电话',
    receiver_address VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    deliver_time DATETIME DEFAULT NULL COMMENT '发货时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT NULL COMMENT '原订单创建时间',
    archive_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单归档表';
