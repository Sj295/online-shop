SET NAMES utf8mb4;

USE online_shop;

-- 管理员账号：admin / 123456
-- 普通用户：user / 123456
-- 密码使用 BCrypt 加密，示例为 $2a$10$ 开头
INSERT INTO ums_user (id, username, password, nickname, phone, email, avatar, role) VALUES
(1, 'admin', '$2a$10$9nAySRZ9GbPmzt./3dgVMekHb7CHpZCi1SSXwgJb2weDjIF6Luyw6', '管理员', '13800000000', 'admin@shop.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin', 1),
(2, 'user', '$2a$10$9nAySRZ9GbPmzt./3dgVMekHb7CHpZCi1SSXwgJb2weDjIF6Luyw6', '用户', '13800000001', 'user@shop.com', 'https://api.dicebear.com/7.x/avataaars/svg?seed=user', 0);

-- 收货地址
INSERT INTO ums_address (user_id, receiver_name, phone, province, city, district, detail, is_default) VALUES
(2, '张三', '13800000001', '北京市', '北京市', '朝阳区', '建国路 88 号 SOHO 现代城 A 座 1001', 1);

-- 商品分类
INSERT INTO pms_category (id, name, parent_id, level, sort, icon) VALUES
(1, '居家生活', 0, 1, 1, 'Sofa'),
(2, '服饰鞋包', 0, 1, 2, 'Shirt'),
(3, '数码电器', 0, 1, 3, 'Smartphone'),
(4, '美妆个护', 0, 1, 4, 'Sparkles'),
(5, '食品饮料', 0, 1, 5, 'Coffee');

-- 轮播图
INSERT INTO pms_carousel (title, image, link, sort) VALUES
('夏季新品', 'https://picsum.photos/seed/carousel1/1200/400', '/category/1', 1),
('精选家居', 'https://picsum.photos/seed/carousel2/1200/400', '/category/2', 2),
('数码礼遇', 'https://picsum.photos/seed/carousel3/1200/400', '/category/3', 3);

-- 商品（使用 4:5 比例图片）
INSERT INTO pms_product (id, category_id, name, subtitle, description, main_image, price, original_price, stock, sale_count, status, is_hot, is_new) VALUES
(1, 1, '北欧简约落地灯', '柔和光线，为客厅增添温暖氛围', '精选金属灯杆与亚麻灯罩，光线柔和不刺眼，适合卧室、客厅阅读角。', 'https://picsum.photos/seed/lamp1/400/500', 299.00, 399.00, 120, 45, 1, 1, 0),
(2, 1, '日式陶瓷花瓶', '手工拉坯，素净温润', '景德镇手工陶瓷，哑光釉面，适合插干花或单独陈列。', 'https://picsum.photos/seed/vase2/400/500', 128.00, 168.00, 200, 89, 1, 0, 1),
(3, 1, '纯棉针织盖毯', '亲肤柔软，四季可用', '100% 新疆长绒棉，细密针织，午休、沙发盖毯皆宜。', 'https://picsum.photos/seed/blanket3/400/500', 189.00, 259.00, 150, 67, 1, 1, 0),
(4, 2, '经典白衬衫', '挺括版型，职场百搭', '高支棉面料，免烫处理，通勤与休闲场景皆可穿着。', 'https://picsum.photos/seed/shirt4/400/500', 199.00, 299.00, 300, 156, 1, 1, 0),
(5, 2, '极简帆布托特包', '大容量，轻便出行', '加厚帆布材质，内置小口袋，通勤、购物一包搞定。', 'https://picsum.photos/seed/bag5/400/500', 159.00, 229.00, 180, 98, 1, 0, 1),
(6, 2, '羊毛混纺围巾', '软糯保暖，秋冬必备', '精选羊毛混纺，触感柔软，纯色设计易于搭配。', 'https://picsum.photos/seed/scarf6/400/500', 139.00, 199.00, 220, 112, 1, 1, 0),
(7, 3, '无线降噪耳机', '静谧聆听，沉浸音乐', '主动降噪技术，40 小时续航，支持多设备切换。', 'https://picsum.photos/seed/headphone7/400/500', 899.00, 1299.00, 80, 234, 1, 1, 0),
(8, 3, '便携式蓝牙音箱', '小体积，大能量', '360 度环绕音效，IPX5 防水，户外露营好伴侣。', 'https://picsum.photos/seed/speaker8/400/500', 349.00, 499.00, 150, 178, 1, 0, 1),
(9, 3, '机械键盘', '段落清晰，码字愉悦', '热插拔轴体，PBT 键帽，支持有线/蓝牙双模。', 'https://picsum.photos/seed/keyboard9/400/500', 459.00, 599.00, 100, 145, 1, 1, 0),
(10, 4, '保湿修护面霜', '深层滋润，修护屏障', '神经酰胺与透明质酸配方，温和适合敏感肌。', 'https://picsum.photos/seed/cream10/400/500', 268.00, 368.00, 160, 321, 1, 1, 0),
(11, 4, '香氛沐浴露', '植物香调，沐浴仪式感', '氨基酸表活，泡沫绵密，洗后不紧绷。', 'https://picsum.photos/seed/shower11/400/500', 89.00, 129.00, 250, 189, 1, 0, 1),
(12, 4, '哑光丝绒口红', '显色不拔干，高级雾面', '丝绒质地，持久显色，多色可选。', 'https://picsum.photos/seed/lipstick12/400/500', 149.00, 219.00, 190, 267, 1, 1, 0),
(13, 5, '精品挂耳咖啡', '现磨风味，随时享用', '精选阿拉比卡豆，中深度烘焙，坚果与巧克力香气。', 'https://picsum.photos/seed/coffee13/400/500', 69.00, 99.00, 300, 445, 1, 1, 0),
(14, 5, '混合坚果礼盒', '每日一把，健康补给', '原味烘焙，不添加防腐剂，独立小包装。', 'https://picsum.photos/seed/nuts14/400/500', 118.00, 168.00, 210, 198, 1, 0, 1),
(15, 5, '高山乌龙茶', '清香回甘，茶韵悠长', '高山茶园直采，传统焙火工艺，耐泡回甘。', 'https://picsum.photos/seed/tea15/400/500', 158.00, 228.00, 180, 156, 1, 1, 0);

-- 商品SKU（每个商品一个默认SKU）
INSERT INTO pms_sku (product_id, sku_code, sku_specs, price, stock, sale_count) VALUES
(1, 'SKU-00001', '{"规格":"默认"}', 299.00, 120, 45),
(2, 'SKU-00002', '{"规格":"默认"}', 128.00, 200, 89),
(3, 'SKU-00003', '{"规格":"默认"}', 189.00, 150, 67),
(4, 'SKU-00004', '{"规格":"默认"}', 199.00, 300, 156),
(5, 'SKU-00005', '{"规格":"默认"}', 159.00, 180, 98),
(6, 'SKU-00006', '{"规格":"默认"}', 139.00, 220, 112),
(7, 'SKU-00007', '{"规格":"默认"}', 899.00, 80, 234),
(8, 'SKU-00008', '{"规格":"默认"}', 349.00, 150, 178),
(9, 'SKU-00009', '{"规格":"默认"}', 459.00, 100, 145),
(10, 'SKU-00010', '{"规格":"默认"}', 268.00, 160, 321),
(11, 'SKU-00011', '{"规格":"默认"}', 89.00, 250, 189),
(12, 'SKU-00012', '{"规格":"默认"}', 149.00, 190, 267),
(13, 'SKU-00013', '{"规格":"默认"}', 69.00, 300, 445),
(14, 'SKU-00014', '{"规格":"默认"}', 118.00, 210, 198),
(15, 'SKU-00015', '{"规格":"默认"}', 158.00, 180, 156);
